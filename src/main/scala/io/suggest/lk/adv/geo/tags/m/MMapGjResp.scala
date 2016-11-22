package io.suggest.lk.adv.geo.tags.m

import io.suggest.sjs.common.geo.json.GjFeature

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.11.16 21:49
  * Description:
  */
object MMapGjResp {

  def apply(raw: js.Dynamic): MMapGjResp = {
    val s = raw
      .asInstanceOf[ js.Array[GjFeature] ]
    apply(s)
  }

}


case class MMapGjResp(arr: IndexedSeq[GjFeature]) {

  def iterator: Iterator[GjFeature] = {
    arr.iterator
      .filter(_ != null)    // TODO Удалить это вместе с iterator(), когда будет решена проблема с сериализацией в LkAdvGeo. Там в конце списка фич null добавлялся из-за проблем с запятыми при поточной json-сериализации.
  }

}

