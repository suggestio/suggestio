package io.suggest.ym.model

import io.suggest.util.CascadingFieldNamer
import cascading.tuple.{TupleEntry, Tuple, Fields}
import com.scaleunlimited.cascading.BaseDatum

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.02.14 13:55
 * Description: Для хранения и представления информации о расположении прайсов на сайтах используется этот датум.
 */
object YmShopPriceUrlDatum extends CascadingFieldNamer {

  val SHOP_ID_FN    = fieldName("shopId")
  val PRICE_URL_FN  = fieldName("priceUrl")
  val AUTH_INFO_FN  = fieldName("authInfo")

  val FIELDS = new Fields(SHOP_ID_FN, PRICE_URL_FN, AUTH_INFO_FN)


  /** Десериализация кортежа. */
  val deserializeAuthInfo: PartialFunction[AnyRef, Option[AuthInfoDatum]] = {
    case null     => None
    case t: Tuple => Some(AuthInfoDatum(t))
  }

  /** Сериализовать auth info в значение для поля AUTH_INFO_FN. */
  def serializeAuthInfo(aid: AuthInfoDatum): Tuple = {
    if (aid == null) {
      null
    } else {
      aid.getTuple
    }
  }

}

import YmShopPriceUrlDatum._

class YmShopPriceUrlDatum extends BaseDatum(FIELDS) {

  def this(t: Tuple) = {
    this
    setTuple(t)
  }

  def this(te: TupleEntry) = {
    this
    setTupleEntry(te)
  }

  def this(shopId:Int, priceUrl:String, authInfo:AuthInfoDatum) = {
    this
    this.shopId = shopId
    this.priceUrl = priceUrl
    this.authInfo = authInfo
  }

  def shopId: Int = _tupleEntry getInteger SHOP_ID_FN
  def shopId_=(shopId: Int) = _tupleEntry.setInteger(SHOP_ID_FN, shopId)

  def priceUrl = _tupleEntry getString PRICE_URL_FN
  def priceUrl_=(priceUrl: String) = _tupleEntry.setString(PRICE_URL_FN, priceUrl)

  def authInfo = {
    val maybeDatum = _tupleEntry.getObject(AUTH_INFO_FN)
    deserializeAuthInfo(maybeDatum)
  }
  def authInfo_=(aid: AuthInfoDatum) = _tupleEntry.setObject(AUTH_INFO_FN, serializeAuthInfo(aid))
}

