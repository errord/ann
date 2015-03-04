package anna.async

import anna.async.NetBuilderOps._
import anna.logger.LOG
import anna.logger.LOG.debug
import anna.data.{ForgetValue, HushValue}
import org.junit.Assert._
import org.junit.Test

import scala.util.Random

class SOSSuite extends MySuite {
  val s = "1,0,0,1,0,0,1,0,0"
  val o = "1,1,0,1,1,0,1,1,0"

  private def dotLineNet(){
    val itm = 3.0
    builder.inputTickMultiplier = itm
    builder.addInput("in")
    // dots
    builder.use("in").chain("mi11",1.0,0.0,HushValue((2 * itm).toInt)).hush("mi11")
                     .chain("mi12",1.0,0.0).loop("loop",1.0,0.0,1.0)
                     .chain("dot",0.6/(2.0*itm),0.6).hush("mi12").hush("loop").hush("dot")
    // lines
    builder.use("in").chain("mi21",0.5,0.55,HushValue(),ForgetValue(0.4 / itm))
                     .hush("mi21")
                     .chain("line",1.0,0.0).hush("line")
                     
    // if line then not dot
    builder.use("line").hush("mi12").hush("loop").hush("dot")
    
    build()
    
    netWrapper.addAfterFire("in"){ println("INCOMING!") }
    
    debug("------------")
  }
  
  @Test def shouldHaveDotsAndLines() = {
    dotLineNet()
    var dots = 0; netWrapper.addAfterFire("dot"){ println("KROPA!"); dots += 1; }
    var lines = 0; netWrapper.addAfterFire("line"){ println("KRECHA!"); lines += 1; }
    init()

    netWrapper += s
    netWrapper.tickUntilCalm()
    println(s"dots: $dots, lines: $lines")
    assertEquals(3, dots)
    assertEquals(0, lines)

    dots = 0; lines = 0;
    netWrapper += o
    netWrapper.tickUntilCalm()
    println(s"dots: $dots, lines: $lines")
    assertEquals(0, dots)
    assertEquals(3, lines)
  }
  
  @Test def shouldGuessDotsAndLines() = {
    dotLineNet()
    val sb = StringBuilder.newBuilder
    netWrapper.addAfterFire("dot"){ sb.append('.') }
    netWrapper.addAfterFire("line"){ sb.append('-') }
    init()
    
    netWrapper += "1,0,0"
    netWrapper.tickUntilCalm()
    assertEquals(".",sb.toString)
    
    sb.clear()
    
    netWrapper += "0,1,0"
    netWrapper.tickUntilCalm()
    assertEquals(".",sb.toString)
    
    sb.clear()
    
    netWrapper += "0,0,1"
    netWrapper.tickUntilCalm()
    assertEquals(".",sb.toString)
    
    sb.clear()
    
    netWrapper += "1,0,1"
    netWrapper.tickUntilCalm()
    assertEquals(".",sb.toString)
    
    sb.clear()
    
    netWrapper += "1,1,0"
    netWrapper.tickUntilCalm()
    assertEquals("-",sb.toString)
    
    sb.clear()
    
    netWrapper += "0,1,1"
    netWrapper.tickUntilCalm()
    assertEquals("-",sb.toString)
    
    sb.clear()
    
    netWrapper += "1,1,1"
    netWrapper.tickUntilCalm()
    assertEquals("-",sb.toString)  
  }
  
  private def SNet(){
    val itm = 3.0
    builder.inputTickMultiplier = itm
    builder.addInput("in")
    // dots
    builder.use("in").chain("mi11",1.0,0.0,HushValue((2 * itm).toInt)).hush("mi11")
                     .chain("mi12",1.0,0.0).loop("loop",1.0,0.0,1.0)
                     .chain("dot",0.6/(2.0*itm),0.6).hush("mi12").hush("loop").hush("dot")
                     .chain("S",0.5,0.81)
    build()
    
    netWrapper.addAfterFire("in"){ println("INCOMING!") }

    debug("------------")
  }
  
  @Test def shouldHaveSInterval3() = {
    SNet()
    
    var dots = 0; netWrapper.addAfterFire("dot"){ println("KROPA!"); dots += 1; } 
    var S = 0; netWrapper.addAfterFire("S"){ println("S!"); S += 1; }
    
    netWrapper += s
    init()
    val interval = netWrapper.tickUntilCalm()
    println(s"interval: $interval, dots: $dots, S: $S")
    assertEquals(3, dots)
    assertEquals(1, S)
    
    dots = 0
    S = 0
    netWrapper += o
    netWrapper.tickUntilCalm()
    println(s"dots: $dots, S: $S")
    assertEquals(3, dots)
    assertEquals(1, S)
  }
  
  private def ONet(){
    val itm = 3.0
    builder.inputTickMultiplier = itm
    builder.addInput("in")
    // lines
    builder.use("in").chain("mi21",0.5,0.55,HushValue(),ForgetValue(0.4 / itm)).hush("mi21")
                     .chain("line",1.0,0.0).hush("line")    
                     .chain("O",0.6,0.81)
    build()
    
    netWrapper.addAfterFire("in"){ println("INCOMING!") }

    debug("------------")
  }
  
  @Test def shouldHaveOInterval3() = {
    ONet()
    
    var lines = 0; netWrapper.addAfterFire("line"){ println("KRECHA!"); lines += 1; }
    var O = 0; netWrapper.addAfterFire("O"){ println("O!"); O += 1; }
    
    netWrapper += o
    init()
    val interval = netWrapper.tickUntilCalm()
    println(s"interval: $interval, lines: $lines, O: $O")
    assertEquals(3, lines)
    assertEquals(1, O)
    
    lines = 0
    O = 0
    netWrapper += s
    netWrapper.tickUntilCalm()
    println(s"lines: $lines, O: $O")
    assertEquals(0, lines)
    assertEquals(0, O)
  }
  
  private def SOSNet(){
    val itm = 3.0
    builder.inputTickMultiplier = itm
    builder.addInput("in")
    // dots
    builder.use("in").chain("mi11",1.0,0.0,HushValue((2 * itm).toInt)).hush("mi11")
                     .chain("mi12",1.0,0.0).loop("loop",1.0,0.0,1.0)
                     .chain("dot",0.6/(2.0*itm),0.6).hush("mi12").hush("loop").hush("dot") 
                     .chain("S",0.5,0.81)
    // lines
    builder.use("in").chain("mi21",0.5,0.55,HushValue(),ForgetValue(0.4 / itm)).hush("mi21")
                     .chain("line",1.0,0.0).hush("line")
                     .chain("O",0.6,0.81)
                     
    // if line then not dot
    builder.use("line").hush("mi12").hush("loop").hush("dot")
    
    build()

    netWrapper.addAfterFire("in"){ println("INCOMING!") }
    
    debug("------------")
  }
  
  @Test def shouldHaveSOSInterval3() = {
    SOSNet()
    
    var dots = 0; netWrapper.addAfterFire("dot"){ println("KROPA!"); dots += 1; } 
    var S = 0; netWrapper.addAfterFire("S"){ println("S!"); S += 1; }
    var lines = 0; netWrapper.addAfterFire("line"){ println("KRECHA!"); lines += 1; }
    var O = 0; netWrapper.addAfterFire("O"){ println("O!"); O += 1; }
    
    init()

    netWrapper += s
    netWrapper.tickUntilCalm()
    println(s"dots: $dots, S: $S, lines: $lines, O: $O")
    assertEquals(3, dots)
    assertEquals(1, S)
    assertEquals(0, lines)
    assertEquals(0, O)
    
    dots = 0; S = 0; lines = 0; O = 0;
    
    netWrapper += o
    netWrapper.tickUntilCalm()
    println(s"dots: $dots, S: $S, lines: $lines, O: $O")
    assertEquals(0, dots)
    assertEquals(0, S)
    assertEquals(3, lines)
    assertEquals(1, O)
    
    dots = 0; S = 0; lines = 0; O = 0;
    
    netWrapper += s
    netWrapper.tickUntilCalm()
    println(s"dots: $dots, S: $S, lines: $lines, O: $O")
    assertEquals(3, dots)
    assertEquals(1, S)
    assertEquals(0, lines)
    assertEquals(0, O)
  }
  
  @Test def shouldHaveSOSTogether() = {
    SOSNet()
    
    val sb = StringBuilder.newBuilder
    netWrapper.addAfterFire("S"){ sb.append('S') }
    netWrapper.addAfterFire("O"){ sb.append('O') }
    
    netWrapper += s
    netWrapper += o
    netWrapper += s
    
    init()
    netWrapper.tickUntilCalm()
    assertEquals("SOS", sb.toString)
  }
  
  private def SOSNetWithHushNeuron(){
    val itm = 3.0
    builder.inputTickMultiplier = itm
    builder.defSlope = 5.0
    builder.addInput("in")
    // dots
    builder.use("in").chain("mi11",1.0,0.0,HushValue((2 * itm).toInt)).hush("mi11")
                     .chain("mi12",1.0,0.0).loop("loop",1.0,0.0,1.0)
                     .chain("dot",0.6/(2.0*itm),0.6)
                     .chain("S",0.5,0.81)
    builder.addHushNeuron("dot_hush").hush("mi12").hush("loop").hush("dot")
    builder.use("dot").hush("dot_hush")
    
    // lines
    builder.use("in").chain("mi21",0.55,0.58,HushValue(),ForgetValue(0.4 / itm)).hush("mi21")
                     .chain("line",1.0,0.0).hush("line")
                     .chain("O",0.6,0.81)
                     
    // if line then not dot
    builder.use("line").hush("dot_hush")

    // if S then not O, if O then not S...
    builder.use("S").chainHushNeuron("hush_letters").hush("S").hush("O")
    builder.use("O").hush("hush_letters")
    
    build()

    netWrapper.addAfterFire("in"){ println("INCOMING!") }
    netWrapper.addAfterFire("dot"){ println("KROPA!") }
    netWrapper.addAfterFire("line"){ println("KRECHA!") }
    
    debug("------------")
  }

  @Test def shouldHaveSOSWithHushNeuron() = {
    SOSNetWithHushNeuron()
    
    val sb = StringBuilder.newBuilder
    netWrapper.addAfterFire("S"){ sb.append('S') }
    netWrapper.addAfterFire("O"){ sb.append('O') }
    
    netWrapper += s
    netWrapper += o
    netWrapper += s
    
    init()
    netWrapper.tickUntilCalm()
    assertEquals("SOS", sb.toString)
  }

  def shouldTestNoise(shouldBe: String, noisedSignal: String, noised1: Double = 0.9, noised0: Double = 0.1) = {
    SOSNetWithHushNeuron()

    val sb = StringBuilder.newBuilder
    netWrapper.addAfterFire("S"){ sb.append('S') }
    netWrapper.addAfterFire("O"){ sb.append('O') }

    netWrapper.regSign('i',noised1)
    netWrapper.regSign('_',noised0)

    LOG.timer()

    netWrapper += noisedSignal
    netWrapper.tickUntilCalm()
    assertEquals(shouldBe, sb.toString)

    LOG.date()
  }

  @Test def shouldUnnoiseS() = shouldTestNoise("S",s)
  @Test def shouldUnnoiseSForwardShift1() = shouldTestNoise("S","0,1,0,0,1,0,0,1,0")
  @Test def shouldUnnoiseSForwardShift2() = shouldTestNoise("S","0,0,1,0,0,1,0,0,1")
  @Test def shouldUnnoiseSNoiseAtTheEnd() = shouldTestNoise("S","1,0,0,1,0,0,1,0,0,1")
  @Test def shouldUnnoiseO() = shouldTestNoise("O",o)
  @Test def shouldUnnoiseOForwardShift1() = shouldTestNoise("O","0,1,1,0,1,1,0,1,1")
  @Test def shouldUnnoiseONoiseAtTheEnd() = shouldTestNoise("O","1,1,0,1,1,0,1,1,0,1")
  @Test def shouldUnnoiseONoiseInTheMiddle() = shouldTestNoise("O","1,1,0,1,1,1,1,1,0")
  @Test def shouldUnnoiseSUnclearSignals() = shouldTestNoise("S","i,_,_,i,_,_,i,_,_")
  @Test def shouldUnnoiseOUnclearSignals() = shouldTestNoise("O","i,i,_,i,i,_,i,i,_")

  @Test def shouldUnnoiseSOS() = {
    val noisedS = "i,_,_,i,_,_,i,_,_"
    val noisedO = "i,i,_,i,i,_,i,i,_"
    shouldTestNoise("SOS",noisedS + "," + noisedO + "," + noisedS)
  }

  @Test def shouldUnnoiseVaried() = {
    def vary(d: Double) = d + (Random.nextDouble() * 0.1 - 0.05)
    def V = vary(0.95)
    def v = vary(0.05)

    val inputSignal = List(V,v,v,V,v,v,V,v,v,V,V,v,V,V,v,V,V,v,V,v,v,V,v,v,V,v,v)

    SOSNetWithHushNeuron()

    val sb = StringBuilder.newBuilder
    netWrapper.addAfterFire("S"){ sb.append('S') }
    netWrapper.addAfterFire("O"){ sb.append('O') }

    LOG.timer()

    inputSignal.foreach( netWrapper += _ )
    netWrapper.tickUntilCalm()
    assertEquals("SOS", sb.toString)

    LOG.date()
  }
}