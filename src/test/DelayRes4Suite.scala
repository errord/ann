package test


import org.scalatest.junit.JUnitSuite
import org.junit.{Test, Before}
import org.junit.Assert._
import main._
import Utils._

class DelayRes4Suite extends JUnitSuite {
  
  // i'm still not sure if it's a good idea to use implicit classes for it...
  implicit class DotLineNetBuilder(builder: NetBuilder){
    def dotLine(startingPoint: String, mp: String, dotEnd: String, lineEnd: String) = { 
      // startingPoint must exist, ending points may or may not exist
      // mp: middle prefix, a prefix for all inner neurons so they won't get confused with any others
      
      //assertions
      assert(builder.middleNeuronType == NeuronType.DELAY, s"The middle neuron type must be DELAY and is ${builder.middleNeuronType}")
      assert(builder.resolution == 4, s"The net resolution must be 4 and is ${builder.resolution}")
      
      builder.throwOnError = false
      
      //dot chain
      builder.use(startingPoint)
             .chainMiddle(s"${mp}11",0.28,0.5)
             .loop(s"${mp}_loop1",1.0,0.5,1.0)
             .chainMiddle(s"${mp}12",1.0,0.9)
             .chainOutput(dotEnd,1.0)
      builder.use(s"${mp}11").setForgetting(0.2)
      builder.use(s"${mp}12").setForgetting(0.2)
      builder.use(s"${mp}12").connect(s"${mp}11", -0.49)
      builder.use(s"${mp}12").connect(s"${mp}_loop1", -1.0)
      builder.use(dotEnd).connect(s"${mp}12", -1.0)
      // line chain
      builder.use(startingPoint)
             .chainMiddle(s"${mp}21",0.19,0.5)
             .chainMiddle(s"${mp}22",1.0,0.5)
             .chainOutput(lineEnd,1.0)
      builder.use(s"${mp}22").connect(s"${mp}21", -0.35)
      // if line then not dot
      builder.use(s"${mp}21").connect(s"${mp}11", -1.0)
      builder.use(s"${mp}21").connect(s"${mp}_loop1", -1.0)
      
      builder.throwOnError = true
      
      builder
    }
  }
  
  private lazy val netRes4 = {
    val builder = NetBuilder(NeuronType.DELAY, 5.0, 0.1, 4)

    builder.addInput("in1")
   
    builder.dotLine("in1","mi","out1","out2")
    
    val (in, net, out) = builder.build("in","out")
    val out1 = builder.get("out1")
    val sb = StringBuilder.newBuilder
    out.addAfterFireTrigger(out1, (n:Neuron) => {
      println("KROPA!")
      sb.append('.'); 
    })
    val out2 = builder.get("out2")
    out.addAfterFireTrigger(out2, (n:Neuron) => {
      println("KRECHA!")
      sb.append('-'); 
    })
    
    (in, sb, net)
  }
  
  @Test
  def shouldDotThenNothingRes4(){
    val (in, sb, _) = netRes4
    
    in += "1,0,0,0,0,0"
    val interval = in.tickUntilCalm()
    assertEquals(".",sb.toString)
    println(s"interval: $interval")
  }
  
  @Test
  def shouldDot3TimesRes4(){
    val (in, sb, _) = netRes4
    
    in += "1,0,0,1,0,0,1,0,0"
    val interval = in.tickUntilCalm()
    assertEquals("...",sb.toString)
    println(s"interval: $interval")
  }
  
  @Test
  def shouldLineThenNothingRes4(){
    val (in, sb, net) = netRes4
    
    val netStateLog = StringBuilder.newBuilder
    val neurons = net.size

    net.addAfterTickTrigger( net => {
      val outputSum = net.outputSum
      val weightSum = net.weightSum
      val iteration = net.iteration
      netStateLog.append(s"#${iteration}: outputSum=${outputSum}, avgOutput=${outputSum/neurons}\n")
    })
    
    in += "1,1,0,0,0,0"
    in.tickUntilCalm()
    println( netStateLog.toString )
    
    net.getNeurons.foreach( n => {
      println(n.id + ": " + n.getClass().getName())
    })
    assertEquals("-",sb.toString)
  }
  
  @Test
  def shouldLine3TimesRes4(){
    val (in, sb, _) = netRes4
    
    in += "1,1,0,1,1,0,1,1,0"
    in.tickUntilCalm()
    assertEquals("---",sb.toString)
  }
    
  @Test
  def shouldLine2TimesWithSpaceRes4(){
    val (in, sb, _) = netRes4
    
    in += "1,1,0,0,1,1,0,0"
    in.tick(12)
    assertEquals("--",sb.toString)
  }
  
  @Test
  def shouldDot2NotLineRes4(){
    val (in, sb, _) = netRes4
    
    in += "1,0,0,1,0,0"
    in.tickUntilCalm()
    assertEquals("..",sb.toString)
  }
  
    @Test
  def shouldDotNotLine1(){
	val (in, sb, _) = netRes4
	
	in += "1,0,0,0,0,0"
	in.tickUntilCalm()
	assertEquals(".",sb.toString)
  }
  
  
  @Test
  def shouldDotNotLine3(){
	val (in, sb, _) = netRes4
	
	in += "1,0,0,1,0,0,1,0,0"
	in.tickUntilCalm()
	assertEquals("...",sb.toString)
  }
  
  @Test
  def shouldLineNotDot1(){
	val (in, sb, _) = netRes4
	LOG.allow("mi11","mi21")
	in += "1,1,0,0,0,0"
	in.tickUntilCalm()
	assertEquals("-",sb.toString)
	LOG.clearAllowedIds()
  }
  
  @Test
  def shouldLineNotDot2(){
	val (in, sb, _) = netRes4
	
	in += "1,1,0,1,1,0"
	in.tickUntilCalm()
	assertEquals("--",sb.toString)
  }
  
  @Test
  def shouldLineNotDot3(){
	val (in, sb, _) = netRes4
	
	in += "1,1,0,1,1,0,1,1,0"
	in.tickUntilCalm()
	assertEquals("---",sb.toString)
  }
  
  @Test
  def shouldDotThenLine(){
	val (in, sb, _) = netRes4
	LOG.allow("mi21","mi11","mi_loop1")
	in += "1,0,0,1,1,0"
	in.tickUntilCalm()
	assertEquals(".-",sb.toString)
	LOG.clearAllowedIds()
  }
  
  @Test
  def shouldDotThenLineThenDot_separate(){
	val (in, sb, _) = netRes4
	
	in += "1,0,0"
	val interval1 = in.tickUntilCalm()
	assertEquals(".",sb.toString)
	in += "1,1,0"
	val interval2 = in.tickUntilCalm()
	assertEquals(".-",sb.toString)
	in += "1,0,0"
	val interval3 = in.tickUntilCalm()
	assertEquals(".-.",sb.toString)
	println(s"intervals: $interval1, $interval2, $interval3")
  }
  
  @Test
  def shouldDotThenLineThenDot_together(){
	val (in, sb, _) = netRes4
	LOG.allow("mi21","mi11","mi_loop1")
	in += "1,0,0,1,1,0,1,0,0"
	in.tickUntilCalm()
	assertEquals(".-.",sb.toString)
	LOG.clearAllowedIds()
  }
  
  @Test
  def shouldDotThenLineThenDotThenLine_together(){
	val (in, sb, _) = netRes4
	in += "1,0,0,1,1,0,1,0,0,1,1,0"
	in.tickUntilCalm()
	assertEquals(".-.-",sb.toString)
  } // another quality to track: whether the net goes overheated, 
    // ie. if after each symbol there's enough leftover signals that
    // it will slowly add up and eventually make a neuron fire by mistake
    // so now it's delay between the end of symbol and recognition of the symbol,
    // the contrast, ie. if the net is configured so that the amount of changes we can make to the weights
    // without affecting the outcome is maximum,
    // and not the overheating.
}