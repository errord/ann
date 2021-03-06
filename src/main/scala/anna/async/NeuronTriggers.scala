package anna.async

import anna.async.NeuronTriggers.Trigger
import anna.async.NeuronTriggers.AfterFireTrigger
import scala.collection.mutable

trait NeuronTriggers {
  private val afterFire = mutable.Map[String, AfterFireTrigger]()
  private val thresholdPassed = mutable.Map[String, Trigger]()
  private val hushRequested = mutable.Map[String, Trigger]()
  
  private def add(id: String, f: Trigger, triggers: mutable.Map[String, Trigger]) =
    if(triggers.contains(id)) throw new IllegalArgumentException(s"There was already registered a trigger $id")
    else triggers.put(id, f)
  private def is(id: String, triggers: mutable.Map[String, Trigger]) = triggers.contains(id) 
  private def remove(id: String, triggers: mutable.Map[String, Trigger]) = triggers.remove(id)
  private def clear(triggers: mutable.Map[String, Trigger]) = triggers.clear()
  private def trigger(triggers: mutable.Map[String, Trigger]) = triggers.values.foreach( _() )
  
  def addAfterFire(id: String, f: AfterFireTrigger) =
    if(afterFire.contains(id)) throw new IllegalArgumentException(s"There was already registered a trigger $id")
    else afterFire.put(id, f)
  def isAfterFire(id: String) = afterFire.contains(id)
  def removeAfterFire(id: String) = afterFire.remove(id)
  def clearAfterFire() = afterFire.clear()
  def triggerAfterFire(output: Double) = afterFire.values.foreach( _(output) )

  def addThresholdPassed(id: String, f: Trigger) = add(id, f, thresholdPassed)
  def isThresholdPassed(id: String) = is(id, thresholdPassed)
  def removeThresholdPassed(id: String) = remove(id, thresholdPassed)
  def clearThresholdPassed() = clear(thresholdPassed) 
  def triggerThresholdPassed() = trigger(thresholdPassed)
  
  def addHushRequested(id: String, f: Trigger) = add(id, f, hushRequested)
  def isHushRequested(id: String) = is(id, hushRequested)
  def removeHushRequested(id: String) = remove(id, hushRequested)
  def clearHushRequested() = clear(hushRequested)
  def triggerHushRequested() = trigger(hushRequested)

  def removeAllTriggers() = {
    afterFire.clear()
    thresholdPassed.clear()
    hushRequested.clear()
  }
}

object NeuronTriggers {
  type Trigger = () => Any
  type AfterFireTrigger = (Double) => Any
}
