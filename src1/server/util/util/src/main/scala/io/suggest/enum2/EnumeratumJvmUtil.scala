package io.suggest.enum2

import enumeratum.values.{IntEnum, IntEnumEntry, ValueEnum, ValueEnumEntry}
import io.suggest.common.empty.EmptyUtil
import play.api.data.Mapping
import play.api.data.Forms._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.08.17 10:10
  * Description: Jvm-only утиль для enumeratum-моделей.
  */
object EnumeratumJvmUtil {


  /** Собрать play form mapping для ValueEnum.
    *
    * @param m Модель со значенями.
    * @tparam ValueType Тип enum-ключа.
    * @tparam EntryType Тип одного элемента enum-модели.
    * @return play form mapping типа EntryType.
    */
  def valueIdMapping[ValueType, EntryType <: ValueEnumEntry[ValueType]]
                    (m: ValueEnum[ValueType, EntryType], mapping0: Mapping[ValueType], fallback: => ValueType): Mapping[EntryType] = {
    mapping0
      .transform[Option[EntryType]](
        m.withValueOpt,
        _.fold(fallback)(_.value)
      )
      .verifying("error.required", _.nonEmpty)
      .transform[EntryType](EmptyUtil.getF, EmptyUtil.someF)
  }


  /** id-маппинг для enum-модели с целочисленными ключами.
    *
    * @param m IntEnum-модель
    * @tparam EntryType Тип значения.
    * @return
    */
  def intIdMapping[EntryType <: IntEnumEntry](m: IntEnum[EntryType]): Mapping[EntryType] = {
    def fallbackF = Int.MinValue
    valueIdMapping(
      m = m,
      mapping0 = {
        val f = EnumeratumUtil.valueF[Int]
        number(
          min = m.values.headOption.fold(fallbackF)(f),
          max = m.values.lastOption.fold(Int.MaxValue)(f)
        )
      },
      fallback = fallbackF
    )
  }

}
