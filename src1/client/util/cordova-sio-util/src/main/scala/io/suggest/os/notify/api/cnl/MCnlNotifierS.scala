package io.suggest.os.notify.api.cnl

import io.suggest.os.notify.MOsToast
import japgolly.univeq._
import monocle.macros.GenLens
import io.suggest.ueq.JsUnivEqUtil._

import scala.collection.immutable.HashMap
import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.03.2020 23:23
  * Description: Модель состояния cordova-notification адаптера.
  */
object MCnlNotifierS {

  def lastId = GenLens[MCnlNotifierS]( _.lastId )
  def toastsById = GenLens[MCnlNotifierS]( _.toastsById )
  def toastUids = GenLens[MCnlNotifierS]( _.toastUids )
  def listeners = GenLens[MCnlNotifierS]( _.listeners )
  def permission = GenLens[MCnlNotifierS]( _.permission )

  @inline implicit def univEq: UnivEq[MCnlNotifierS] = UnivEq.force

}


/** Контейнер данных состояния адаптера нотификации.
  *
  * @param lastId Внутренний счётчик. Используется для id уведомлений.
  *               Хранит последний использованный id.
  * @param listeners Листенеры событий.
  *                  Функции могут иметь разную arity, поэтому тут максимально абстрактная js-функция.
  * @param toastUids cnl использует integer id. А у нас тут string id.
  *                  Поэтому, нужен транслятор между id'шниками.
  * @param toastsById Нотификации по cnl-id.
  * @param permission Последнее известное состояние пермишшена на раздачу уведомлений.
  */
case class MCnlNotifierS(
                          lastId          : Int                                 = 0,
                          toastsById      : HashMap[Int, MOsToast]              = HashMap.empty,
                          toastUids       : HashMap[String, Int]                = HashMap.empty,
                          listeners       : HashMap[String, js.Function]        = HashMap.empty,
                          permission      : Option[Boolean]                     = None,
                        )
