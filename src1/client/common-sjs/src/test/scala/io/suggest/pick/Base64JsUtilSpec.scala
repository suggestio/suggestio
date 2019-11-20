package io.suggest.pick

import minitest._
import org.scalajs.dom.raw.Blob
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.concurrent.Future
import scala.scalajs.js.typedarray.Uint8Array

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.09.17 16:16
  * Description: Тесты для [[Base64JsUtil]].
  */
object Base64JsUtilSpec extends SimpleTestSuite {

  // TODO Тест конвертеров base64-URL в блоб

  private def imagePngBytes = Array(
    0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00, 0x00, 0x00, 0x0d, 0x49, 0x48, 0x44, 0x52,
    0x00, 0x00, 0x00, 0x05, 0x00, 0x00, 0x00, 0x05, 0x08, 0x06, 0x00, 0x00, 0x00, 0x8d, 0x6f, 0x26,
    0xe5, 0x00, 0x00, 0x00, 0x1c, 0x49, 0x44, 0x41, 0x54, 0x08, 0xd7, 0x63, 0xf8, 0xff, 0xff, 0x3f,
    0xc3, 0x7f, 0x06, 0x20, 0x05, 0xc3, 0x20, 0x12, 0x84, 0xd0, 0x31, 0xf1, 0x82, 0x58, 0xcd, 0x04,
    0x00, 0x0e, 0xf5, 0x35, 0xcb, 0xd1, 0x8e, 0x0e, 0x1f, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4e,
    0x44, 0xae, 0x42, 0x60, 0x82
  ).map(i => i.toShort)

  private val ct = "image/png"
  private val b64Url = {
    // TODO Генерить base64-строку на основе бинаря выше по тексту.
    // В оригинале было == в конце, что порождало 0x00 0x00 в конце бинаря. Удалено.
    "data:" + ct + ";base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg"
  }
  private val sz = imagePngBytes.length


  /** Сравнить указанный блоб и сорец выше. */
  private def _matchBlob(blob: Blob): Future[Unit] = {
    assertEquals( blob.`type`, ct)
    for (arrBuf <- JsBinaryUtil.blob2arrBuf( blob )) yield {
      val arr8 = new Uint8Array(arrBuf)
      val expectedArr = imagePngBytes
      for (i <- 0 until sz) {
        //println(s"i=$i uint8a=${arr8(i)} expected=${expectedArr(i)}")
        assertEquals(arr8(i), expectedArr(i))
      }
      assertEquals(blob.size, sz)
    }
  }


  testAsync(s"b64Url2BlobPlainConv() against $sz-bytes Blob") {
    val blob = Base64JsUtil.b64Url2BlobPlainConv( b64Url )
    _matchBlob( blob )
  }

  // TODO Хром отказывается работать. А в ноде v8, который из того же днищща, и видимо поэтому тест не работает.
  /*
  test(s"b64Url2BlobUsingFetch() against $sz-bytes Blob") {
    Base64JsUtil.b64Url2BlobUsingXHR( b64Url )
      .flatMap { _matchBlob }
  }
  */

  // TODO node.js test env работает через одно место, поэтому $g.fetch is not a function. Надо бы починить. npm install node-fetch в корне не помогает.
  /*
  test(s"b64Url2BlobUsingFetch() against $sz-bytes Blob") {
    Base64JsUtil.b64Url2BlobUsingXHR( b64Url )
      .flatMap { _matchBlob }
  }
  */

  testAsync(s"b64Url2Blob($sz bytes PNG)") {
    Base64JsUtil.b64Url2Blob( b64Url )
      .flatMap( _matchBlob )
  }

}
