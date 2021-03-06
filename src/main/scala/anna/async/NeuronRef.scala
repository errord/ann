package anna.async

import akka.actor.{ActorRef, actorRef2Scala}
import akka.pattern.ask
import anna.Context
import anna.async.Messages._
import anna.logger.LOG._
import anna.utils.Utils.await

class NeuronRef(val id: String, val ref: ActorRef) {
  def info = await[NeuronInfo](ref, GetData)
  def getSynapses = await[MsgSynapses](ref, GetSynapses).synapses
  def setSynapses(synapses: Seq[Synapse]) = if(synapses.nonEmpty) ref ! SetSynapses(synapses)

  def setFriends(friends: Set[String]) = ref ! SetFriends(friends)

  def hush() = {
    ref ! HushRequest
  }
  
  protected def calculateOutput = Double.NaN // we don't do that here 
  
  def addAfterFire(triggerId: String)(f: (Double) => Any) = await[Answer](ref, AddAfterFireTrigger(triggerId, f)) match {
    case Success(id) => true
    case Failure(str) => error(this,s"addAfterFire failure: $str"); false
  }
  def removeAfterFire(name: String) = await[Answer](ref, RemoveAfterFireTrigger(name)) match {
    case Success(id) => true
    case Failure(str) => error(this,s"removeAfterFire failure: $str"); false    
  }
  def addHushRequested(triggerId: String)(f: => Any) = await[Answer](ref, AddHushRequestedTrigger(triggerId, () => f)) match {
    case Success(id) => true
    case Failure(str) => error(this,s"addHushRequested failure: $str"); false
  }
  def removeHushRequested(name: String) = await[Answer](ref, RemoveHushRequestedTrigger(name)) match {
    case Success(id) => true
    case Failure(str) => error(this,s"removeHushRequested failure: $str"); false    
  }
  def removeAllTriggers() = await[Answer](ref, RemoveAllTriggers) match {
    case Success(id) => true
    case Failure(str) => error(this,s"removeAllTriggers failure: $str"); false
  }

  def reset() = await[Answer](ref, Reset) match {
    case Success(id) => true
    case Failure(str) => error(this,s"reset failure: $str"); false
  }
  
  def +=(signal: Double) = ref ! Signal(signal, id)
  def !(any: Any) = ref ! any
}
