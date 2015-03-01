package anna.async

import anna.Context.tickTime
import anna.utils.Utils._

import scala.collection.mutable

class NetWrapper(val net: NetRef, val inputTickMultiplier: Double) {
  lazy val ids = net.inputIds
  lazy val size = net.inputSize
  
  private val inputQueue = mutable.Queue[Seq[Double]]()
  
  private var _iteration = 0
  def iteration = _iteration 
  
  def find(id: String) = if(ids.contains(id)) net.find(id).neuronOpt.get
                         else throw new IllegalArgumentException(s"There is no output neuron with id $id")
    
  def add(input: Seq[Double]) = {
	  assert(input.length == size, s"The input vector has to be exactly ${size} numbers long and is ${input.length}.")
    inputQueue += input
  }
  
  def addEmptyInput = add(generateEmptyInput)
  def +=(input: Seq[Double]) = add(input)
  def +=(d: Double) = add(Seq(d))
  def +=(t: (Double,Double)) = add(Seq(t._1,t._2))
  def +=(t: (Double,Double,Double)) = add(Seq(t._1,t._2,t._3))
  
  def generateEmptyInput:Seq[Double] = for(i <- 1 to size) yield 0.0
  
  private val signRegister = mutable.Map[Char,Double]()
  
  def regSign(sign: Char,input: Double) = signRegister += (sign -> input)
  def +=(input: String) = input.split(",").toSeq.map( 
    _.toCharArray().toSeq.map( c => 
      if(signRegister.contains(c)) signRegister(c) 
      else throw new IllegalArgumentException(s"No input registered with sign $c")
  )).foreach( add )

  def tick():Unit = tick(1)
  def tick(n: Int):Unit = for(i <- 1 to n) yield {
    val input = if(inputQueue.nonEmpty) inputQueue.dequeue else generateEmptyInput
    net.signal(input)
    Thread.sleep((inputTickMultiplier * tickTime).toLong)
    _iteration = _iteration + 1
  }
  
  def tickUntilCalm(timeout: Int = 100) = {
    var neuronFired = false
    val neurons = net.getNeurons
    
    neurons.foreach(_.addAfterFire("tickUntilCalm"){ neuronFired = true })
    
    var (calmTick, counter) = (0, 0)
    while(inputQueue.nonEmpty || (calmTick < 3 && counter < timeout)){
      neuronFired = false
      tick()
      if(neuronFired) calmTick = 0 else calmTick += 1
      counter += 1
    }
    
    net.reset()

    neurons.foreach(_.removeAfterFire("tickUntilCalm"))
    counter
  }

  def addAfterFire(id: String, name: String)(f: => Any): Unit = net.addAfterFire(id, name)(f)
  def addAfterFire(id: String)(f: => Any): Unit = addAfterFire(id, id)(f)

  def addHushRequested(id: String, name: String)(f: => Any): Unit = net.addHushRequested(id, name)(f)
  def addHushRequested(id: String)(f: => Any):Unit  = addHushRequested(id, id)(f)

  def shutdown() = net.shutdown()
  def reset() = net.reset()
  def removeAllTriggers() = net.removeAllTriggers()

  def empty = inputQueue.isEmpty
}

object NetWrapper {
  def apply(net: NetRef, inputTickMultiplier: Double) = {
    val ani = new NetWrapper(net, inputTickMultiplier)
    ani.regSign('0',0.0)
    ani.regSign('1', 1.0)
    ani
  }
}