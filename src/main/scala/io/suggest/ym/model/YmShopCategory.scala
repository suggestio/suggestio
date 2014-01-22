package io.suggest.ym.model

import io.suggest.util.CascadingFieldNamer
import cascading.tuple.{TupleEntry, Tuple, Fields}
import com.scaleunlimited.cascading.BaseDatum

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.01.14 11:17
 * Description: Категория уровня магазина.
 */
object YmShopCategory extends CascadingFieldNamer {
  val ID_FN         = fieldName("id")
  val NAME_FN       = fieldName("name")
  val PARENT_ID_FN  = fieldName("parentId")
  // Непонятные поля yid/tid пропущены.

  val FIELDS = new Fields(ID_FN, NAME_FN, PARENT_ID_FN)
}


import YmShopCategory._

class YmShopCategory extends BaseDatum(FIELDS) {

  def this(t: Tuple) = {
    this
    setTuple(t)
  }

  def this(te: TupleEntry) = {
    this
    setTupleEntry(te)
  }

  def this(id:String, name:String, parentIdOpt:Option[String] = None) = {
    this
    setId(id)
    setName(name)
    setParentId(parentIdOpt)
  }


  def getId = _tupleEntry getString ID_FN
  def setId(id: String) = {
    _tupleEntry.setString(ID_FN, id)
    this
  }

  def getName = _tupleEntry getString NAME_FN
  def setName(name: String) = {
    _tupleEntry.setString(NAME_FN, name)
    this
  }

  def getParentId = Option(_tupleEntry getString PARENT_ID_FN)
  def setParentId(parentIdOpt: Option[String]) = {
    _tupleEntry.setString(PARENT_ID_FN, parentIdOpt getOrElse null)
    this
  }

}