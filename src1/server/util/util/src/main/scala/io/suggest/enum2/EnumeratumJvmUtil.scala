package io.suggest.enum2

import enumeratum.{EnumEntry, Enum}
import enumeratum.values.{IntEnum, IntEnumEntry, ValueEnum, ValueEnumEntry}
import io.suggest.common.empty.EmptyUtil
import io.suggest.model.play.qsb.QueryStringBindableImpl
import play.api.data.Mapping
import play.api.data.Forms._
import play.api.mvc.QueryStringBindable

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


  /** Сборка QueryStringBindable для моделей enumeratum ValueEnum.
    *
    * @param m Модель со всеми инстансами.
    *          Ленивое, потому что не факт, что она потребуется.
    * @param vQsb QSB-биндер для типа ключа.
    * @tparam V Тип ключа.
    * @tparam VEE Тип одного значения enum-модели.
    * @return QSB для элементов модели.
    */
  def valueEnumQsb[V, VEE <: ValueEnumEntry[V]](m: => ValueEnum[V, VEE])
                                               (implicit vQsb: QueryStringBindable[V]): QueryStringBindable[VEE] = {
    _anyEnumQsb[V, VEE](m.withValueOpt, _.value)
  }

  /** Сборка QueryStringBindable для моделей enumeratum Enum.
    *
    * @param m Enum-модель со строковыми ключами.
    *          Ленивое, потому что не факт, что она потребуется.
    * @param strQsb QSB для строк.
    * @tparam EE Тип элемента enum-модели.
    * @return QSB[EE].
    */
  def enumQsb[EE <: EnumEntry](m: => Enum[EE])
                              (implicit strQsb: QueryStringBindable[String]): QueryStringBindable[EE] = {
    _anyEnumQsb[String, EE](m.withNameOption, _.entryName)
  }


  /** Сборка QSB для произвольных enum-подобных моделей.
    *
    * @param withValueOpt Функция поиска значения в enum-модели.
    * @param vee2v Получение ключа из элемента enum-модели.
    * @param vQsb QSB-биндер для ключей элементов модели.
    * @tparam V Тип ключей модели.
    * @tparam VEE Тип элементов модели.
    * @return QSB для элементов enum-модели.
    */
  private def _anyEnumQsb[V, VEE](withValueOpt: V => Option[VEE], vee2v: VEE => V)
                                 (implicit vQsb: QueryStringBindable[V]): QueryStringBindable[VEE] = {
    new QueryStringBindableImpl[VEE] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, VEE]] = {
        for {
          vEith <- vQsb.bind(key, params)
        } yield {
          vEith.right.flatMap { v =>
            withValueOpt(v).toRight("e.invalid")
          }
        }
      }

      override def unbind(key: String, value: VEE): String = {
        vQsb.unbind(key, vee2v(value))
      }
    }
  }

}
