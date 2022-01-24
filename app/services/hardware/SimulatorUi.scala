package services.hardware

import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.gui2._
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import config.SignConfig
import models.Image

import java.io.IOException

class SimulatorUi(signs: Seq[SignConfig])(implicit materializer: Materializer) {
  private val (gui, window, screen, labelsLookup, messageButton) = setup()

  val imagesSink: Sink[(SignConfig, Image), _] = Sink.foreach {
    case (sign, image) => {
      val img = if (sign.flip) image.rotate90().rotate90() else image
      val label = labelsLookup.get(sign.address)
      label.map(_.setText(img.toString))
    }
  }

  val indicatorSink: Sink[Boolean, _] = Sink.foreach {
    case true  => messageButton.setLabel("MESSAGE")
    case false => messageButton.setLabel("message")
  }

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
    val (mainPanel, labelsLookup, messageButton) = createContent(window)
    window.setComponent(mainPanel)

    // Create gui and start gui
    val gui = new MultiWindowTextGUI(
      screen,
      new DefaultWindowManager(),
      new EmptySpace(TextColor.ANSI.BLUE)
    )
    (gui, window, screen, labelsLookup, messageButton)
  }

  private def createContent(window: Window) = {
    // Create panel to hold all components
    val mainPanel = new Panel()
    mainPanel.setLayoutManager(new LinearLayout(Direction.VERTICAL))

    // Create sub-panels for each "sign"
    val labels = signs
      .map(s => {
        val label =
          new Label(Seq.fill(s.size._2)(" " * s.size._1).mkString("\n"))
        val panel = new Panel()
        panel.addComponent(label)
        mainPanel.addComponent(panel.withBorder(Borders.singleLine(s.name)))
        s.address -> label
      })
      .toMap

    // Create a button for reading a message
    val messageButton = new Button("Message")
    mainPanel.addComponent(messageButton)

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
    (mainPanel, labels, messageButton)
  }
}
