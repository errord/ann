package anna.data

import anna.Context
import anna.async._
import anna.utils.Utils.formats
import org.json4s.native.Serialization.{read, writePretty}

case class NeuronData(
    id: String,
    threshold: Double,
    hushValue: HushValue,
    forgetting: ForgetTrait,
    synapses: List[SynapseData],
    neuronType: NeuronType,
    activationFunctionName: String,
    friends: Set[String]
){
  def withId(id: String) = copy(id = id)
  def withThreshold(threshold: Double) = copy(threshold = threshold)
  def withHushValue(hushValue: HushValue) = copy(hushValue = hushValue)
  def withForgetting(forgetting: ForgetTrait) = copy(forgetting = forgetting)
  def withSynapses(synapses: List[SynapseData]) = copy(synapses = synapses)
  def withoutSynapses = withSynapses(Nil)
  def withNeuronType(neuronType: NeuronType) = copy(neuronType = neuronType)
  def withActivationFunctionName(activationFunctionName: String) = copy(activationFunctionName = activationFunctionName)
  def withFriends(friends: Set[String]) = copy(friends = friends)
  def withoutFriends = withFriends(Set.empty[String])

  def toJson = writePretty(this)

  def isConnectedTo(id: String) = synapses.find(_.neuronId == id) != None
}

object NeuronData {
  def apply(id: String,
            threshold: Double,
            hushValue: HushValue,
            forgetting: ForgetTrait,
            synapses: List[SynapseData],
            activationFunctionName: String,
            friends: Set[String]
           ):NeuronData
  = apply(id, threshold, hushValue, forgetting, synapses, NeuronTypeStandard(), activationFunctionName, friends)

  def apply(id: String, 
            threshold: Double,
            hushValue: HushValue, 
            forgetting: ForgetTrait,
            synapses: List[SynapseData],
            activationFunctionName: String):NeuronData
    = apply(id, threshold, hushValue, forgetting, synapses, NeuronTypeStandard(), activationFunctionName, Set.empty[String])

  def apply(id: String, 
            threshold: Double,
            hushValue: HushValue,
            forgetting: ForgetTrait,
            activationFunctionName: String):NeuronData
    = apply(id, threshold, hushValue, forgetting, Nil, NeuronTypeStandard(), activationFunctionName, Set.empty[String])

  def apply(id: String,  
            hushValue: HushValue):NeuronData
    = apply(id, 0.0, hushValue, ForgetAll(), Nil, NeuronTypeDummy(), ActivationFunction.UNUSED, Set.empty[String])
  
  def apply(id: String):NeuronData
    = apply(id, 0.0, Context().hushValue, ForgetAll(), Nil, NeuronTypeHush(), ActivationFunction.UNUSED, Set.empty[String])

  def fromJson(jsonStr: String) = read[NeuronData](jsonStr)
}