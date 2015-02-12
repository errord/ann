package anna.async

import akka.actor._
import anna.async.Messages._
import anna.data.{ForgetTrait, HushValue, NeuronData}
import anna.logger.LOG._

import scala.collection.mutable

class Net(val id: String) extends Actor {
  private val neurons = mutable.ListBuffer[NeuronRef]()
  private var ins = List[NeuronRef]()
  
  def receive: Receive = {
    case GetId => sender ! Msg(0.0, id)
    case GetNeurons => sender ! MsgNeurons(neurons.toList)
    case CreateNeuron(data) => createNeuron(data)
    case Shutdown => shutdown()
    case GetInputs => sender ! MsgNeurons(inputs)
    case SetInputs(ids) => setInputs(ids)
    case GetNeuron(id) => sender ! MsgNeuron(findRef(id))
    case SignalSeq(in) => signal(in)
    case Reset => resetBuffer()
    case RemoveAllTriggers => removeTriggers()
  }
  
  def shutdowning(caller: ActorRef): Receive = {
    case NeuronShutdownDone(id) =>
      remove(id)
      if(neurons.isEmpty){
        caller ! NetShutdownDone(id) 
        context.stop(self)
      }
  }

  def waiting(caller: ActorRef, waitingFor: Set[String], title: String = ""): Receive = {
    case Success(id) =>
      if(title.nonEmpty) debug(this, s"received Success in $title from $id, still waitingFor: $waitingFor")
      val newWaitingFor = waitingFor - id
      if(newWaitingFor.isEmpty){
        caller ! Success(id)
        context.become(receive)
      } else context.become(waiting(caller, newWaitingFor, title))
  }
  
  private def inputs = ins.toList

  private def middles = {
    val inputIds = ins.map( _.id ).toSet
    neurons.filterNot( n => inputIds.contains(n.id) ).toList
  }

  private def remove(id: String) = findRef(id) match {
    case Some(ref) => neurons -= ref
    case None => 
  }

  private def shutdown() = {
    debug(s"shutdown for $id")
    context.become( shutdowning(sender) )
    neurons.foreach(_ ! NeuronShutdown)
  }

  private def resetBuffer() = {
    debug(s"reset buffer for $id")
    context.become( waiting(sender, neurons.map(_.id).toSet, "resetting") )
    neurons.foreach(_ ! Reset)
  }

  private def removeTriggers() = {
    debug(s"removing triggers for $id")
    context.become( waiting(sender, neurons.map(_.id).toSet, "removing triggers") )
    neurons.foreach(_ ! RemoveAllTriggers)
  }
  
  private def add(id: String, ref: ActorRef) = {
    val neuronRef = new NeuronRef(id, ref)
    neurons += neuronRef
    sender ! neuronRef
  }

  private def createNeuron(data:NeuronData) = data.neuronType match {
    case NeuronType.STANDARD => createStandard(data.id, data.threshold, data.slope, data.hushValue, data.forgetting, data.tickTimeMultiplier)
    case NeuronType.DUMMY => createDummy(data.id, data.hushValue, data.tickTimeMultiplier)
    case NeuronType.HUSH => createHush(data.id)
  }

  private def createStandard(id: String,
                           threshold: Double,
                           slope: Double,
                           hushValue: HushValue,
                           forgetting: ForgetTrait,
                           tickTimeMultiplier: Double){
	  val ref = context.actorOf(Props(new Neuron(id, threshold, slope, hushValue, forgetting, tickTimeMultiplier)))
    add(id, ref)
  }

  private def createDummy(id: String, hushValue: HushValue, tickTimeMultiplier: Double){
	  val ref = context.actorOf(Props(new DummyNeuron(id, tickTimeMultiplier)))
    add(id, ref)
  }
  
  private def createHush(id: String){
    val ref = context.actorOf(Props(new HushNeuron(id)))
    add(id, ref)
  }
  
  private def signal(in: Seq[Double]){
    assert(in.size == ins.size, s"Difference in size between the input layer (${ins.size}) and the input (${in.size})")
    debug(this, s"signal received in $id: " + in.mkString(", "))
    ins.zip(in).foreach( tuple => tuple._1 += tuple._2 )
  }
  
  private def setInputs(ids: Seq[String]){
    debug(this, s"input layer set in $id: " + ids.mkString(", "))
    ins = neurons.filter( n => ids.contains(n.id) ).toList
    if(ins.size != ids.size){
      val inIds = ins.map( _.id )
      val notFound = ids.filterNot( inIds.contains(_) )
      sender ! Failure(s"setInputs, unable to find neurons with ids: $notFound")
    } else {
      sender ! Success("setInputLayer_" + id)
    }
  }
  
  private def findRef(id: String):Option[NeuronRef] = neurons.find(_.id == id)
}