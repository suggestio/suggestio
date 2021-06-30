package io.suggest.enum2

import enumeratum.values._
import io.suggest.xplay.psb.PathBindableImpl
import io.suggest.xplay.qsb.CrossQsBindable
import play.api.data.Mapping
import play.api.data.Forms._
import play.api.mvc.{PathBindable, QueryStringBindable}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.08.17 10:10
  * Description: Jvm-only утиль для enumeratum-моделей.
  */
object EnumeratumJvmUtil {

  /** Собрать play form mapping для ValueEnum.
    *
    * Можно использовать FormMappingUtil.optMapping2required() для избавления от Option[].
    *
    * @param m Модель со значенями.
    * @tparam ValueType Тип enum-ключа.
    * @tparam EntryType Тип одного элемента enum-модели.
    * @return play form mapping типа EntryType.
    */
  def valueIdOptMapping[ValueType, EntryType <: ValueEnumEntry[ValueType]]
                       (m: ValueEnum[ValueType, EntryType], mapping0: Mapping[ValueType], fallback: => ValueType): Mapping[Option[EntryType]] = {
    mapping0.transform[Option[EntryType]](
      m.withValueOpt,
      _.fold(fallback)(_.value)
    )
  }


  /** id-маппинг для enum-модели с целочисленными ключами.
    *
    * @param m IntEnum-модель
    * @tparam EntryType Тип значения.
    * @return
    */
  def intIdOptMapping[EntryType <: IntEnumEntry](m: IntEnum[EntryType]): Mapping[Option[EntryType]] = {
    def fallbackF = Int.MinValue
    valueIdOptMapping(
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


  /** Option[String] mapping на базе StringEnum[].
    * Для раскрытия Option[] можно использовать FormMappingUtil.optMapping2required().
    */
  def stringIdOptMapping[EntryType <: StringEnumEntry](m: StringEnum[EntryType]): Mapping[Option[EntryType]] = {
    valueIdOptMapping(
      m = m,
      mapping0 = {
        text(
          minLength = 1,
          maxLength = m.values
            .headOption
            .fold(1)( _ => m.values.iterator.map(_.value.length).max )
        )
      },
      fallback = ""
    )
  }


  /** id-маппинг для enum-модели с целочисленными ключами.
    *
    * @param m IntEnum-модель
    * @tparam EntryType Тип значения.
    * @return
    */
  def shortIdOptMapping[EntryType <: ShortEnumEntry](m: ShortEnum[EntryType]): Mapping[Option[EntryType]] = {
    def fallbackF = Short.MinValue
    valueIdOptMapping(
      m = m,
      mapping0 = {
        val f = EnumeratumUtil.valueF[Short]
        number(
          min = m.values.headOption.fold(fallbackF)(f),
          max = m.values.lastOption.fold(Short.MaxValue)(f)
        )
          .transform[Short](_.toShort, _.toInt)
      },
      fallback = fallbackF
    )
  }



  /** Сборка QueryStringBindable для моделей enumeratum ValueEnum.
    *
    * @param adapter Enum model adapter.
    * @param valueQsb QS-binder for enum entry values.
    * @return QSB implementation for related enum model.
    */
  def valueEnumQsb[ValueType, EntryType]
                  (adapter: EnumeratumUtil.Adapter[ValueType, EntryType])
                  (implicit valueQsb: QueryStringBindable[ValueType]): CrossQsBindable[EntryType] = {
    EnumeratumUtil.qsBindable( adapter )( valueQsb: CrossQsBindable[ValueType] )
  }


  def valueEnumPb[ValueType, EntryType <: ValueEnumEntry[ValueType]]
                 (m: => ValueEnum[ValueType, EntryType])
                 (implicit valuePb: PathBindable[ValueType]): PathBindable[EntryType] = {
    _anyEnumPb[ValueType, EntryType]( m.withValueOpt, _.value )
  }

  /** Сборка play router PathBindable.
    *
    * @param withValueOpt Поиск значения по ключу.
    * @param vee2k Извлечение ключа из значения.
    * @param kPb PathBindable ключа.
    * @tparam ValueType Тип ключа.
    * @tparam EntryType Тип (класс) инстансов значения модели.
    * @return PathBindable значения.
    */
  private def _anyEnumPb[ValueType, EntryType]
                        (withValueOpt: ValueType => Option[EntryType], vee2k: EntryType => ValueType)
                        (implicit kPb: PathBindable[ValueType]): PathBindable[EntryType] = {
    new PathBindableImpl[EntryType] {
      override def bind(key: String, value: String): Either[String, EntryType] = {
        kPb.bind(key, value)
          .flatMap { k =>
            withValueOpt(k)
              .toRight("Undefined value")
          }
      }

      override def unbind(key: String, value: EntryType): String = {
        val k = vee2k(value)
        kPb.unbind(key, k)
      }
    }
  }

}
