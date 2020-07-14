package io.suggest.sc.m.inx.save

import io.suggest.common.empty.{EmptyProduct, EmptyUtil}
import io.suggest.conf.ConfConst
import io.suggest.kv.MKvStorage
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.07.2020 15:52
  * Description: Сериализуемая модель последних индексов, хранимая в MKvStorage (localStorage).
  * Это JSON-модель, в которой хранится список последних значимых состояний выдачи.
  * Данные по карточкам - должны быть в кжше браузера, а тут - лишь общая информация.
  */

object MIndexesRecent {

  def empty = apply()

  /** Максимальное кол-во сохранённых элементов в индексе. */
  def MAX_RECENT_ITEMS = 10


  object Fields {
    def LASTS = "l"
  }

  implicit def lastIndexesJson: OFormat[MIndexesRecent] = {
    val F = Fields
    (__ \ F.LASTS).formatNullable[List[MIndexInfo]]
      .inmap[List[MIndexInfo]](
        EmptyUtil.opt2ImplEmptyF( Nil ),
        lasts => Option.when(lasts.nonEmpty)(lasts)
      )
      .inmap(apply, _.recents)
  }

  @inline implicit def univEq: UnivEq[MIndexesRecent] = UnivEq.derive


  /** Сохранить указанное состояние. */
  def save(li: MIndexesRecent): Unit = {
    MKvStorage.save(
      MKvStorage(
        key = ConfConst.SC_INDEXES_RECENT,
        value = li,
      )
    )
  }


  /** Прочитать сохранённое ранее значение. */
  def get(): Option[MIndexesRecent] = {
    MKvStorage
      .get[MIndexesRecent]( ConfConst.SC_INDEXES_RECENT )
      .map(_.value)
  }


  def recents = GenLens[MIndexesRecent](_.recents)

}


/** Контейнер данных по недавним индексам.
  *
  * @param recents Массив последних элементов, новые - сначала.
  */
final case class MIndexesRecent(
                                 recents        : List[MIndexInfo]          = Nil,
                               )
  extends EmptyProduct
