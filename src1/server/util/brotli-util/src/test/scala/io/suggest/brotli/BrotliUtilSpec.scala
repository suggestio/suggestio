package io.suggest.brotli

import java.util

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.util.ByteString
import io.suggest.streams.ByteStringsChunker
import org.meteogroup.jbrotli.BrotliStreamDeCompressor
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.duration._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.02.18 20:11
  * Description: Тесты для [[BrotliUtil]].
  */
class BrotliUtilSpec extends AnyFlatSpec {

  implicit val system = ActorSystem("TestSystem")
  implicit val materializer = ActorMaterializer()


  private def DATA_252 = {
    (0 to 252)
      .iterator
      .map(_.toByte)
      .toArray
  }

  "Streamed brotli compressor" should "handle simple data" in {
    val flow = Flow[ByteString]
      .via( ByteStringsChunker(8192) )
      .via( BrotliUtil.compress )

    val in = Source(0 to 100)
      .map { _ => ByteString.fromArrayUnsafe(DATA_252) }
    val sink = Sink.fold[ByteString, ByteString]( ByteString.empty ) { _ ++ _ }

    val fut = in
      .via(flow)
      .toMat(sink)(Keep.right)
      .run()

    assert( fut.isReadyWithin( 500.millis ) )

    whenReady(fut) { compressedByteString =>
      assert( compressedByteString.nonEmpty )
      // Расжать и убедиться, что всё корректно:
      val dec = new BrotliStreamDeCompressor
      try {
        val resChunk = DATA_252
        val decBuf = DATA_252
        dec.deCompress( compressedByteString.toArray, decBuf )
        var i = 0
        while (dec.needsMoreOutput() || i > 101) {
          assert( util.Arrays.equals(resChunk, decBuf) )
          dec.deCompress( Array.empty[Byte], decBuf )
          i += 1
        }
        assert( !dec.needsMoreInput() )
        assert( !dec.needsMoreOutput() )
      } finally {
        dec.close()
      }
    }
  }

}
