package io.suggest.model

import org.scalatest._
import MPict._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.12.13 18:45
 * Description:
 */
class MImgThumbTest extends FlatSpec with Matchers {

  "MImgThumb" should "statically convert URLs into b64-ids" in {
    val f = { url:String => idBin2Str(imgUrl2id(url)) }
    // TODO нужно запускать их всех паралелльно
    f("http://aversimage.ru/images/2222222222222222222222222222222222222.jpg") should equal ("AsTyP-AsNJ0WvHVVzshkpRXJ6Tc")
    f("http://aversimage.ru/images/2222222222222222222222222222222222222.jpg") should equal ("AsTyP-AsNJ0WvHVVzshkpRXJ6Tc")
    f("http://aversimage.ru/images/ffcfbca69bda450ca0a5b9184014227bklava.jpg") should equal ("vy9APcYxr2WibFxyO3S722hKXWI")
    f("http://aversimage.ru/images/ffcfbca69bda450ca0a5b9184014227bklava.jpg") should equal ("vy9APcYxr2WibFxyO3S722hKXWI")
    f("http://aversimage.ru/images/2222222222222222222222222222222222222.jpg") should equal ("AsTyP-AsNJ0WvHVVzshkpRXJ6Tc")
  }

}
