package io.suggest.sc.index

import io.suggest.common.empty.{EmptyProduct, EmptyUtil}
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

object MScIndexes {

  def empty = apply()

  /** Максимальное кол-во сохранённых элементов в индексе.
    * 7 - примерно высота экрана до наступления скроллинга (с учётом других пунктов меню). */
  def MAX_RECENT_ITEMS = 7


  object Fields {
    def LASTS = "l"
  }

  implicit def lastIndexesJson: OFormat[MScIndexes] = {
    val F = Fields
    (__ \ F.LASTS).formatNullable[List[MScIndexInfo]]
      .inmap[List[MScIndexInfo]](
        EmptyUtil.opt2ImplEmptyF( Nil ),
        lasts => Option.when(lasts.nonEmpty)(lasts)
      )
      .inmap(apply, _.indexes)
  }

  @inline implicit def univEq: UnivEq[MScIndexes] = UnivEq.derive


  def indexes = GenLens[MScIndexes](_.indexes)

}


/** Контейнер данных по недавним индексам.
  *
  * @param indexes Массив индексов.
  */
final case class MScIndexes(
                             indexes        : List[MScIndexInfo]          = Nil,
                           )
  extends EmptyProduct
