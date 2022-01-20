package services.hardware

import com.googlecode.lanterna.{TerminalSize, TextColor}
import com.googlecode.lanterna.gui2._
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import config.SignConfig

import java.io.IOException

class SimulatorUi(signs: Seq[SignConfig]) {
  run()

  private def run(): Unit = {
    // Setup terminal and screen layers
    val terminal = new DefaultTerminalFactory().createTerminal()
    val screen = new TerminalScreen(terminal)
    screen.startScreen()

    // Create window to hold the panel
    val window = new BasicWindow()
    window.setComponent(createContent(window))

    // Create gui and start gui
    val gui = new MultiWindowTextGUI(
      screen,
      new DefaultWindowManager(),
      new EmptySpace(TextColor.ANSI.BLUE)
    );
    gui.addWindowAndWait(window)

    if (screen != null) {
      try {
        screen.stopScreen()
      } catch {
        case e: IOException => e.printStackTrace()
      }
    }
  }

  private def createContent(window: Window) = {
    // Create panel to hold all components
    val mainPanel = new Panel()
    mainPanel.setLayoutManager(new LinearLayout(Direction.VERTICAL))

    // Create sub-panels for each "sign"
    val labels = signs.map(s => {
      val label = new Label(Seq.fill(s.size._2)(" " * s.size._1).mkString("\n"))
      val panel = new Panel()
      panel.addComponent(label)
      mainPanel.addComponent(panel.withBorder(Borders.singleLine(s.name)))
      s.name -> label
    })

    // Create a button for reading a message
    mainPanel.addComponent(new Button("Message"))

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
    mainPanel
  }
}
