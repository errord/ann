package anna

import anna.async.NetBuilder
import anna.data.NetData
import anna.epengine._
import anna.utils.Utils
import anna.logger.LOG._
import anna.async.NetBuilderOps._

/**
 * Created by gorywoda on 06.06.15.
 */
object Commands {

  def context = Context()

  def listEvolutions = {
    val dirs = Utils.listDirs(Context().evolutionDir)
    println(s"the evolutions directory is ${Context().evolutionDir}")
    println(s"evolutions found: ${dirs.size}")
    dirs.foreach( println )
  }

  def listSets = {
    val sets = Utils.listFiles(Context().exercisesSetDir)
    println(s"the exercises sets directory is ${Context().exercisesSetDir}")
    println(s"sets found: ${sets.size}")
    sets.map(s => s.substring(0,s.lastIndexOf(".json"))).foreach( println )
  }

  private var exercisesSetOpt:Option[ExercisesSet] = None

  def set(name: String):Unit = {
    exercisesSetOpt = Some(ExercisesSet.load(name))
  }

  def set:String = exercisesSetOpt.map(_.name).getOrElse("The exercises set is not set")

  private var engineOpt:Option[Engine] = None

  val dotLineData =
    NetBuilder().addInput("in").chain("mi11",1.0,0.5).chain("mi12",1.0,0.5).chain("dot",1.0,0.5)
    .use("in").chain("mi21",1.0,0.5).chain("mi22",1.0,0.5).chain("line",1.0,0.5)
    .use("mi12").hush("mi21")
    .use("mi21").hush("mi11")
    .use("dot").hush("line")
    .use("line").hush("dot")
    .setName("dotline").data

  val simplestData = NetBuilder().addInput("in").chain("dot",1.0,0.5).use("in").chain("line",1.0,0.5).setName("simplest").data
  val zeroData = NetBuilder().addInput("in").chain("dot",0.0,0.0).use("in").chain("line",0.0,0.0).setName("zero").data

  val sosNetData = NetBuilder().SOSNetData()

  private var accessMapOpt: Option[Map[String, MutationAccess]] = None

  def accessMap:String = accessMapOpt.map(_.toString).getOrElse("Access map not set")

  def accessMap(map: Map[String, MutationAccess]):Unit = {
    accessMapOpt = Some(map)
  }

  def setDotLineAccessMap() = accessMap(Map("in" -> MutationAccessDontMutate(), "dot" -> MutationAccessDontDelete(), "line" -> MutationAccessDontDelete()))

  private var inputIdsOpt:Option[List[String]] = None

  def inputIds(ids:String*): Unit ={
    inputIdsOpt = Some(ids.toList)
  }

  def inputIds:String = inputIdsOpt.map(_.mkString(", ")).getOrElse("Input ids not set")

  private var outputIdsOpt:Option[List[String]] = None

  def outputIds(ids:String*): Unit ={
    outputIdsOpt = Some(ids.toList)
  }

  def outputIds:String = outputIdsOpt.map(_.mkString(", ")).getOrElse("Output ids not set")

  def setDotLineConfig() = {
    setDotLineAccessMap()
    inputIds("in")
    outputIds("dot", "line")
    println("done")
  }

  def create(name: String, template: NetData) = {
    val inputIds:List[String] = inputIdsOpt.getOrElse(throw new IllegalArgumentException("No input ids set"))
    val outputIds:List[String] = outputIdsOpt.getOrElse(throw new IllegalArgumentException("No output ids set"))
    val exercisesSet = exercisesSetOpt.getOrElse(throw new IllegalArgumentException("No exercises set... set"))
    engineOpt = Some(Engine(name, inputIds, outputIds, template, exercisesSet))
    println("done")
  }

  def open(name: String) = {
    engineOpt = Some(Engine(name))
    println("done")
  }

  def run(iterations: Int) = engineOpt match {
    case Some(engine) => engine.run(iterations)
    case None => exception(this, "Unable to run as no engine is ready")
  }

  def best = engineOpt match {
    case Some(engine) => engine.best.data
    case None => throw new IllegalArgumentException("Unable to get the best genome as no engine is ready")
  }

  def see(data: NetData):Unit = println(data.toJson)
  def see(context: Context):Unit = println(context.toJson)

  def test(data: NetData, exerciseName: String):Unit = {
    val exercise = ExercisesLibrary(exerciseName)
    val coach = Coach(List(exercise))
    val result = coach.test(data)
    println("--- result: " + result)
  }

  def test(data: NetData):Unit = {
    val result = exercisesSetOpt match {
      case Some(exercisesSet) => Coach(exercisesSet).test(data)
      case None => throw new IllegalArgumentException("No exercises set... set")
    }
    println("--- result: " + result)
  }

  def mutate(data: NetData) = {
    val accessMap = accessMapOpt.getOrElse(throw new IllegalArgumentException("No access map set"))
    val genome = NetGenome(data, accessMap)
    genome.mutate()
    genome.data
  }

  def cross(data1: NetData, data2: NetData):(NetData,NetData) = {
    val accessMap = accessMapOpt.getOrElse(throw new IllegalArgumentException("No access map set"))
    val genome1 = NetGenome(data1, accessMap)
    val genome2 = NetGenome(data2, accessMap)
    if(genome1.crossable(genome2)) {
      val (newGenome1, newGenome2) = genome1.cross(genome2)
      (newGenome1.data, newGenome2.data)
    } else throw new IllegalArgumentException(s"${data1.id} not crossable with ${data2.id}")
  }

  def diff(data1: NetData, data2: NetData) ={
    val sb = StringBuilder.newBuilder
    if(data1.id != data2.id) sb.append(s"net id: ${data1.id} -> ${data2.id}\n")
    if(data1.neurons.size != data2.neurons.size) sb.append(s"#neurons: ${data1.neurons.size} -> ${data2.neurons.size}\n")

    val data1NeuronIds = data1.neurons.map(_.id).toSet
    val data2NeuronIds = data2.neurons.map(_.id).toSet
    if(data1NeuronIds != data2NeuronIds){
      sb.append(s"neurons: ${data1NeuronIds.toList.sorted} -> ${data2NeuronIds.toList.sorted}")
      (data1NeuronIds -- data2NeuronIds).foreach( nid => {
        val n = data1.neuron(nid)
        sb.append(s"$nid deleted: $n\n")
      })
      (data2NeuronIds -- data1NeuronIds).foreach( nid => {
        val n = data2.neuron(nid)
        sb.append(s"$nid added: $n\n")
      })
    }

    data1NeuronIds.intersect(data2NeuronIds).foreach(nid => {
      val n1 = data1.neuron(nid)
      val n2 = data2.neuron(nid)
      if(n1 != n2){
        if(n1.threshold != n2.threshold) sb.append(s"$nid threshold: ${n1.threshold} -> ${n2.threshold}\n")
        if(n1.slope != n2.slope) sb.append(s"$nid slope: ${n1.slope} -> ${n2.slope}\n")
        if(n1.hushValue != n2.hushValue) sb.append(s"$nid hushValue: ${n1.hushValue} -> ${n2.hushValue}\n")
        if(n1.forgetting != n2.forgetting) sb.append(s"$nid forgetting: ${n1.forgetting} -> ${n2.forgetting}\n")
        if(n1.tickTimeMultiplier != n2.tickTimeMultiplier) sb.append(s"$nid tickTimeMultiplier: ${n1.tickTimeMultiplier} -> ${n2.tickTimeMultiplier}\n")
        // probably wrong
        if(n1.synapses != n2.synapses) sb.append(s"$nid synapses: ${n1.synapses} -> ${n2.synapses}\n")
      }
    })

    sb.toString
  }

}
