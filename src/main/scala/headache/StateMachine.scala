package headache

/**
 * Simple implementation of a state machine. Notice that state keeping is not thread safe (it wouldn't make much sense either). If you
 * need to use the state machine from different threads, override apply and synchronize it calling super.apply
 */
trait StateMachine[A] extends PartialFunction[A, Unit] {

  case class Transition(name: String, f: PartialFunction[A, Transition]) extends PartialFunction[A, Transition] {
    def apply(a: A) = f(a)
    def isDefinedAt(a: A) = f.isDefinedAt(a)
    def orElse(t: Transition) = Transition(name + "+" + t.name, f orElse t)

    override def toString = name
  }
  def transition(f: PartialFunction[A, Transition]) = {
    val name = new Exception().getStackTrace.drop(3).take(1).map(caller => caller.getFileName + ":" + caller.getLineNumber).mkString("\n")
    Transition(name, f)
  }
  def namedTransition(name: String)(f: PartialFunction[A, Transition]) = Transition(name, f)

  private[this] var curr: Transition = initState
  def initState: Transition
  def apply(a: A): Unit = {
    val prev = curr
    try {
      val newCurr = curr(a)
      if (curr == prev) curr = newCurr //only update state if it was not updated by reentrantcy of this method
    } catch {
      case e: MatchError => throw new MatchError(e.getMessage + " not handled by state " + curr.name)
    }
  }
  def applyIfDefined(a: A) = if (isDefinedAt(a)) apply(a)
  def isDefinedAt(a: A) = curr.isDefinedAt(a)

  /**
   * @return the current transition
   */
  def current = curr
  /**
   * Imperative change of state. Discouraged but unavoidable sometimes.
   */
  def switchTo(t: Transition) = curr = t

  /**
   * Done state, which is defined for no payload
   */
  lazy val done = Transition("done", PartialFunction.empty)
}