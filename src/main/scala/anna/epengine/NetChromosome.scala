package anna.epengine

import anna.data.{NeuronData, NetData}
import anna.utils.DoubleRange._
import anna.logger.LOG._

/**
 * Created by gorywoda on 04.01.15.
 */

class NetChromosome(private var _data: NetData, val accessMap: Map[String, MutationAccess.Value]){
  import NetChromosome._

  override def clone = NetChromosome(_data, accessMap)

  def id = _data.id
  def neurons = _data.neurons
  def inputs = _data.inputs
  def inputTickMultiplier = _data.inputTickMultiplier
  def data = _data
  def find(id: String) = _data.neurons.find( _.id == id )

  def findSynapse(from: String, to: String) = find(from) match {
    case Some(n) => n.synapses.find( _.neuronId == to )
    case None => None
  }

  def mutate() = Probability.performRandom(
    (addNeuronProbability, addNeuron _),
    (deleteNeuronProbability, deleteNeuron _),
    (mutateNeuronProbability, mutateNeuron _),
    (inputTickMultiplierProbability, mutateInputTickMultiplier _)
  )

  private def deleteNeuron(id: String): Unit = {
    _data = _data.withNeurons( neurons.filterNot( _.id == id ) )
  }

  private def addNeuron(n: NeuronData) = {
    _data = _data.withNeurons(n :: neurons)
  }

  private def updateNeuron(neuronData: NeuronData) = {
    deleteNeuron(neuronData.id)
    addNeuron(neuronData)
  }

  private def addNeuron(): Unit ={
    debug(this, "addNeuron, current size of neurons is: " + neurons.size)
    // 1. create a new neuron with name id+neurons.size and according to NeuronChromosome specifications
    val newNCh = Engine().tossForNeuron(id + neurons.size, accessMap)
    debug(this,s"a new neuron created with id ${newNCh.id}")
    // 2. create exactly one synapse from the set of all neurons (a synapse from an output neuron is also ok)

    val oldNCh = NeuronChromosome(neurons(RandomNumber(neurons.size)), accessMap)
    debug(this,s"connecting ${oldNCh.id} with the new neuron")
    oldNCh.addSynapse(Engine().tossForSynapse(newNCh.id))
    updateNeuron(oldNCh.data)
    // 3. create exactly one synapse from the new neuron to the set of not-input neurons
    val mutNs = mutableNeurons
    if(mutNs.nonEmpty) {
      val connectToId = mutNs(RandomNumber(mutNs.size)).id
      newNCh.addSynapse(Engine().tossForSynapse(connectToId))
      debug(this, s"connecting the new neuron to $connectToId")
    }

    addNeuron(newNCh.data)
    debug(this,"the new neuron was added to the net, the number of neurons is now: " + neurons.size)
  }

  private def deleteSynapsesTo(neuronId: String) = neurons.map( n => if(n.isConnectedTo(id)){
    val nCh = NeuronChromosome(n)
    nCh.deleteSynapse(id)
    Some(nCh.data)
  } else None).flatten.foreach( n => updateNeuron(n) )

  private def deleteNeuron(): Unit ={
    val faN = fullAccessNeurons
    if(faN.nonEmpty){
      val id = faN(RandomNumber(faN.size)).id
      deleteNeuron(id)
      deleteSynapsesTo(id)
    }
  }

  private def mutateNeuron(): Unit ={
    val mutNs = mutableNeurons
    if(mutNs.nonEmpty){
      val nCh = NeuronChromosome(mutNs(RandomNumber(mutNs.size)))
      nCh.mutate()
      updateNeuron(nCh.data)
    }
  }

  private def mutateInputTickMultiplier(): Unit ={
    _data = _data.withInputTickMultiplier(inputTickMultiplierRange.choose(RandomNumber()))
  }

  private def fullAccessNeurons = neurons.filter( n => accessMap.getOrElse(n.id, MutationAccess.FULL) == MutationAccess.FULL)
  private def mutableNeurons = neurons.filter( n => accessMap.getOrElse(n.id, MutationAccess.FULL) != MutationAccess.DONTMUTATE)
}

object NetChromosome {
  var addNeuronProbability = Probability(0.1)
  var deleteNeuronProbability = Probability(0.1)
  var mutateNeuronProbability = Probability(0.75)
  var inputTickMultiplierProbability = Probability(0.05)
  var inputTickMultiplierRange = 2.0 <=> 4.0

  def apply(data: NetData, accessMap: Map[String, MutationAccess.Value]) = new NetChromosome(data, accessMap)
  def apply(id: String, neurons: List[NeuronData], inputs: List[String], inputTickMultiplier: Double):NetChromosome =
    NetChromosome(NetData(id, neurons, inputs, inputTickMultiplier), Map())
}
