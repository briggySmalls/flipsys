class Sequencer {
  private var currentValue = 0L
  def nextSeq() = {
    val ret = currentValue
    currentValue += 1
    ret
  }
}
