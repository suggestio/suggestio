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
class CompressUtilJvm {

  /** Неявная утварь всякая. */
  object Implicits {

    /** Gzip-сжимающий конвертер. */
    implicit object GzipCompressConv extends IDataConv[ByteBuffer, ConvCodecs.Gzip, Array[Byte]] {
      override def convert(uncompressed: ByteBuffer): Array[Byte] = {
        val byteArr = BinaryUtil.byteBufToByteArray( uncompressed )
        val baos = new ByteArrayOutputStream( byteArr.length / 2 )
        val gzipper = new GZIPOutputStream( baos )
        gzipper.write( byteArr, 0, byteArr.length )
        gzipper.finish()
        baos.toByteArray
      }
    }


    /** Gunzip-разжимающий конвертер. */
    implicit object GunzipDeCompressConv extends IDataConv[Array[Byte], ConvCodecs.Gzip, ByteBuffer] {
      override def convert(bytes: Array[Byte]): ByteBuffer = {
        val gunzipper = new GZIPInputStream(
          new ByteArrayInputStream( bytes )
        )
        val baos = new ByteArrayOutputStream( bytes.length * 3 )
        IOUtils.copy(gunzipper, baos)
        val barr = baos.toByteArray
        ByteBuffer.wrap( barr )
      }
    }

  }


}
