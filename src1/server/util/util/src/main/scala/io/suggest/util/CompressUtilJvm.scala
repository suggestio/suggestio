package io.suggest.util

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.ByteBuffer
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import io.suggest.bin.{BinaryUtil, ConvCodecs, IDataConv}
import org.apache.commons.io.IOUtils

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.05.17 17:04
  * Description: Утиль для сжатия/разжатия данных на стороне JVM.
  */
final class CompressUtilJvm {

  def gzip(uncompressed: Array[Byte]): Array[Byte] = {
    val baos = new ByteArrayOutputStream( uncompressed.length / 2 )
    val gzipper = new GZIPOutputStream( baos )
    gzipper.write( uncompressed, 0, uncompressed.length )
    gzipper.finish()
    baos.toByteArray
  }


  def gunzip(compressed: Array[Byte]): Array[Byte] = {
    val gunzipper = new GZIPInputStream(
      new ByteArrayInputStream( compressed )
    )
    val baos = new ByteArrayOutputStream( compressed.length * 3 )
    IOUtils.copy(gunzipper, baos)
    baos.toByteArray
  }


  /** Неявная утварь всякая. */
  object Implicits {

    /** Gzip-сжимающий конвертер. */
    implicit object GzipCompressConv extends IDataConv[ByteBuffer, ConvCodecs.Gzip, Array[Byte]] {
      override def convert(uncompressed: ByteBuffer): Array[Byte] = {
        val byteArr = BinaryUtil.byteBufToByteArray( uncompressed )
        gzip( byteArr )
      }
    }


    /** Gunzip-разжимающий конвертер. */
    implicit object GunzipDeCompressConv extends IDataConv[Array[Byte], ConvCodecs.Gzip, ByteBuffer] {
      override def convert(bytes: Array[Byte]): ByteBuffer = {
        ByteBuffer.wrap( gunzip(bytes) )
      }
    }

  }

}
