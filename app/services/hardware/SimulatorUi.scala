package services.hardware

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.gui2._
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import config.SignConfig
import models.Image

import java.io.IOException

class SimulatorUi(private val signs: Seq[SignConfig])(implicit
    materializer: Materializer
) {
  private val (
    gui,
    window,
    screen,
    _imagesSink,
    _indicatorSink,
    _buttonSource
  ) =
    setup()

  val imagesSink = _imagesSink
  val indicatorSink = _indicatorSink
  val buttonSource = _buttonSource

  def run(): Unit = {
    gui.addWindowAndWait(window)

    if (screen != null) {
      try {
        screen.stopScreen()
      } catch {
        case e: IOException => e.printStackTrace()
      }
    }
  }

  private def setup() = {
    // Setup terminal and screen layers
    val terminal = new DefaultTerminalFactory().createTerminal()
    val screen = new TerminalScreen(terminal)
    screen.startScreen()

    // Create window to hold the panel
    val window = new BasicWindow()
    val (imagesSink, indicatorSink, buttonSource) = createContent(window, signs)

    // Create gui and start gui
    val gui = new MultiWindowTextGUI(
      screen,
      new DefaultWindowManager(),
      new EmptySpace(TextColor.ANSI.BLUE)
    )
    (gui, window, screen, imagesSink, indicatorSink, buttonSource)
  }

  private def createContent(window: Window, signs: Seq[SignConfig]) = {
    // Create panel to hold all components
    val mainPanel = new Panel()
    window.setComponent(mainPanel)
    mainPanel.setLayoutManager(new LinearLayout(Direction.VERTICAL))

    val imagesSink = signElements(mainPanel, signs)
    val (indicatorSink, buttonSource) = controlElements(mainPanel)

    mainPanel.addComponent(
      new Button(
        "Close",
        new Runnable() {
          override def run(): Unit = {
            window.close();
          }
        }
      )
    )
    (imagesSink, indicatorSink, buttonSource)
  }

  private def signElements(mainPanel: Panel, signs: Seq[SignConfig]) = {
    // Create sub-panels for each "sign"
    val labelsLookup = signs
      .map(s => {
        val label =
          new Label(Seq.fill(s.size._2)(" " * s.size._1).mkString("\n"))
        val panel = new Panel()
        panel.addComponent(label)
        mainPanel.addComponent(panel.withBorder(Borders.singleLine(s.name)))
        s.address -> label
      })
      .toMap

    Sink.foreach[(SignConfig, Image)] { case (sign, image) =>
      val img = if (sign.flip) image.rotate90().rotate90() else image
      val label = labelsLookup.get(sign.address)
      label.map(_.setText(img.toString))

    }
  }

  private def controlElements(mainPanel: Panel) = {
    val (buttonQueue, buttonSource) = Source.queue[Boolean](16).preMaterialize()
    // Create a button for reading a message
    val messageButton = new Button(
      "Message",
      new Runnable {
        override def run(): Unit = buttonQueue.offer(true)
      }
    )
    mainPanel.addComponent(messageButton)

    val indicatorSink = Sink.foreach[Boolean] {
      case true  => messageButton.setLabel("MESSAGE")
      case false => messageButton.setLabel("message")
    }

    (indicatorSink, buttonSource)
  }
}

object SimulatorUi {}
