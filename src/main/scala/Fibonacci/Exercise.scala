package Fibonacci

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, UniformFanInShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, MergePreferred, RunnableGraph, Sink, Source, ZipWith}

object Exercise extends App {
  implicit val system = ActorSystem("FibonacciExercise")
  implicit val materializer = ActorMaterializer()

  val fibonacciGraph = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._
    val zipShape = builder.add(ZipWith[Int, Int, (Int, Int)]((a,b) => (a,b)))
    val mergePreferredShape = builder.add(MergePreferred[(Int,Int)](1))
    val logicFlowShape = builder.add(Flow[(Int, Int)].collect{
      case (current, previous) =>
        Thread.sleep(2000)
        (current+previous, current)
    })
    val broadcast = builder.add(Broadcast[(Int, Int)](2))
    val result = builder.add(Flow[(Int, Int)].map(_._1))

    zipShape.out ~> mergePreferredShape ~> logicFlowShape ~> broadcast ~> result
                    mergePreferredShape.preferred         <~ broadcast

    UniformFanInShape(result.out, zipShape.in0, zipShape.in1)
  }

  val resultGraph = RunnableGraph.fromGraph(
    GraphDSL.create(){ implicit builder =>
      import GraphDSL.Implicits._
      val firstInputShape = builder.add(Source.single[Int](1))
      val secondInputShape = builder.add(Source.single[Int](1))
      val fibonacciShape = builder.add(fibonacciGraph)
      val sinkShape = builder.add(Sink.foreach[Int](println))

      firstInputShape ~> fibonacciShape.in(0)
      secondInputShape ~> fibonacciShape.in(1)
      fibonacciShape.out ~> sinkShape

      ClosedShape
    }
  )
  resultGraph.run()
}
