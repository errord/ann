package anna.epengine

import anna.Context
import anna.data.NetData
import anna.utils.Utils.formats
import org.json4s.native.Serialization.{read, writePretty}
/**
 * Created by gorywoda on 13.02.15.
 */
case class GenomePoll(genomes: List[NetGenome]){
  def apply(id: String):NetGenome = genomes.find(_.id == id).getOrElse(throw new IllegalArgumentException(s"There is no genome with id $id"))
  def apply(index: Int):NetGenome = genomes(index)
  def size = genomes.size
  def ids = genomes.map(_.id)
  def empty = genomes.isEmpty

  def toJson = writePretty(this)

  def genomesSorted(results:Map[String,Double]) =
    genomes.sortWith( (g1: NetGenome, g2: NetGenome) => results(g1.id) > results(g2.id))
}

object GenomePoll {
  def apply(template: NetData,
            inputIds: List[String],
            outputIds: List[String],
            size: Int,
            mutationsProfile: MutationsProfile = MutationsProfile.noMutations,
            initialMutationsNumber: Int = Context().initialMutationsNumber):GenomePoll = {
    assert(Context().synapsesDensity >= 1.0, "There should be at least one synapse for neuron, is: " + Context().synapsesDensity)
    assert(inputIds.size + outputIds.size <= Context().neuronsRange.end, s"You chose ${inputIds.size} inputs and ${outputIds.size} outputs, but the max possible neurons number is only ${Context().neuronsRange.end}")

    GenomePoll(newGeneration(template, AccessMap(inputIds, outputIds), size, mutationsProfile, initialMutationsNumber))
  }

  def apply(genomes: NetGenome*):GenomePoll = GenomePoll(genomes.toList)

  def newGeneration(template: NetData,
                    accessMap: Map[String, MutationAccess],
                    size: Int,
                    mutationProfile: MutationsProfile,
                    initialMutationsNumber: Int = Context().initialMutationsNumber):List[NetGenome] =
    (1 to size).map( i => {
      val ng = NetGenome(template, accessMap).netId(template.id+i)
      for(j <- 1 to initialMutationsNumber) mutationProfile.mutate(ng)
      ng
    }).toList

  def fromJson(jsonStr: String) = read[GenomePoll](jsonStr)
}