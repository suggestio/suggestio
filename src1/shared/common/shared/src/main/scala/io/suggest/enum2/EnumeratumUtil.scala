package io.suggest.enum2

import enumeratum._
import enumeratum.values.{StringEnum, StringEnumEntry, ValueEnum, ValueEnumEntry}
import io.suggest.common.empty.EmptyUtil
import io.suggest.url.bind.{QsBindable, QsBinderF, QsUnbinderF}
import play.api.libs.json._

import scala.language.implicitConversions

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.06.17 18:32
  * Description: Серверная утиль для enumeratum.
  * Например, нормальная поддержка play-json для кросс-платформенных моделей.
  */
object EnumeratumUtil {

  /** Поддержка JSON-десериализации из строки.
    *
    * @param model Enum-модель, содержащая все инстансы.
    * @tparam T Тип одного элемента модели.
    * @return JSON Reads.
    */
  def enumEntryReads[T <: EnumEntry](model: Enum[T]): Reads[T] = {
    _optReads2reads {
      implicitly[Reads[String]]
        .map(model.withNameOption)
    }
  }

  def valueEnumEntryReads[V, VEE <: ValueEnumEntry[V]](model: ValueEnum[V, VEE])(implicit rawReads: Reads[V]): Reads[VEE] = {
    _optReads2reads {
      rawReads
        .map( model.withValueOpt )
    }
  }

  def _optReads2reads[T](rOpt: Reads[Option[T]]): Reads[T] = {
    rOpt
      .filter( JsonValidationError("error.unknown.id") )(_.nonEmpty)
      .map( EmptyUtil.getF )
  }


  /** Abstracted body for QueryStringBindable.bind() method: make raw query-string value binded into EnumEntry or ValueEnumEntry.
    *
    * @param adapter Enum [[Adapter]] impl.
    * @param valueB ValueType bindable between URL-query-string and ValueType.
    * @tparam ValueType Type of value enum. key.
    * @tparam EntryType Type of each enum element, keyed by ValueType
    * @return URL query-string binding function.
    */
  def qsBindable[ValueType, EntryType](adapter: Adapter[ValueType, EntryType])
                                      (implicit valueB: QsBindable[ValueType]): QsBindable[EntryType] = {
    new QsBindable[EntryType] {
      override def bindF: QsBinderF[EntryType] = { (key, params) =>
        for {
          valueE <- valueB.bindF(key, params)
        } yield {
          for {
            value <- valueE
            entry <- adapter
              .withValueOpt(value)
              .toRight( "Unknown value" )
          } yield entry
        }
      }
      /** Abstracted body for QueryStringBindable.unbind() method: URL query-string unbinding. */
      override def unbindF: QsUnbinderF[EntryType] = { (key, value) =>
        valueB.unbindF( key, adapter.entryToValue(value) )
      }
    }
  }

  /** Поддержка JSON-сериализации в строку.
    *
    * @tparam T Тип элемента enum-модели.
    * @return JSON Writes.
    */
  implicit def enumEntryWrites[T <: EnumEntry]: Writes[T] = {
    implicitly[Writes[String]]
      .contramap( _.entryName )
  }

  implicit def valueEnumEntryWrites[T, VEE <: ValueEnumEntry[T]](implicit writes: Writes[T]): Writes[VEE] = {
    writes
      .contramap( valueF )
  }


  /** JSON Format на базе Reads и Writes.
    *
    * @param model Enum-модель, содержащая все элементы.
    * @tparam T Тип одного элемента модели.
    * @return JSON Format.
    */
  def enumEntryFormat[T <: EnumEntry](model: Enum[T]): Format[T] = {
    Format(enumEntryReads(model), enumEntryWrites)
  }
  def valueEnumEntryFormat[V, VEE <: ValueEnumEntry[V]](model: ValueEnum[V, VEE])(implicit format: Format[V]): Format[VEE] = {
    Format(valueEnumEntryReads(model), valueEnumEntryWrites)
  }


  /** Просто функция для доступа к value-ключу. */
  def valueF[ValueType]: ValueEnumEntry[ValueType] => ValueType = {
    _.value
  }

  /** Просто функция для доступа к value-ключу. */
  def entryNameF: EnumEntry => String = {
    _.entryName
  }

  /** При рендере html-шаблонов в play с помощью @select() бывает удобно сериализовать модель в список option'ов. */
  def toSelectOptions[EE <: EnumEntry](m: Enum[EE]): Seq[(String, String)] = {
    toSelectOptions(m.values) { ee =>
      val k = ee.entryName
      val v = ee.toString
      (k, v)
    }
  }
  def toSelectOptions[V, VEE <: ValueEnumEntry[V]](m: ValueEnum[V, VEE]): Seq[(String, String)] = {
    toSelectOptions(m.values) { vee =>
      val k = vee.value.toString
      val v = vee.toString
      (k, v)
    }
  }
  def toSelectOptions[EE](items: IterableOnce[EE])(kvF: EE => (String, String)): Seq[(String, String)] = {
    items
      .iterator
      .map(kvF)
      .toSeq
  }


  implicit class ValueEnumEntriesOps[V](val vees: IterableOnce[ValueEnumEntry[V]]) extends AnyVal {

    def onlyIds: Iterator[V] = {
      vees
        .iterator
        .map(_.value)
    }

  }


  object MapJson {

    implicit def stringEnumKeyMapReads[K <: StringEnumEntry: StringEnum, V: Reads]: Reads[Map[K, V]] = {
      Reads.mapReads[K, V] { str =>
        implicitly[StringEnum[K]]
          .withValueOpt(str)
          .fold[JsResult[K]](JsError("invalid"))(JsSuccess(_))
      }
    }

    implicit def stringEnumKeyMapWrites[K <: StringEnumEntry, V: Writes]: Writes[Map[K, V]] = {
      val w = implicitly[Writes[V]]
      OWrites[Map[K, V]] { kvsMap =>
        val jsObjFields = kvsMap.iterator
          .map { case (k, v) =>
            k.value -> w.writes(v)
          }
          .toSeq
        JsObject( jsObjFields )
      }
    }

  }



  sealed trait Adapter[ValueType, EntryType] {
    def withValueOpt(value: ValueType): Option[EntryType]
    def entryToValue(entry: EntryType): ValueType
    def theEnum: AnyRef

    override final def toString = theEnum.toString
  }
  object Adapter {

    implicit final class ValueEnumAdp[ValueType, EntryType <: ValueEnumEntry[ValueType]]( override val theEnum: ValueEnum[ValueType, EntryType] )
      extends Adapter[ValueType, EntryType] {
        override def withValueOpt(value: ValueType ): Option[EntryType] =
          theEnum.withValueOpt( value )
        override def entryToValue(entry: EntryType): ValueType =
          entry.value
      }


    implicit final class EnumAdp[EntryType <: EnumEntry]( override val theEnum: Enum[EntryType] )
      extends Adapter[String, EntryType] {
        override def withValueOpt(value: String ): Option[EntryType] =
          theEnum.withNameOption( value )
        override def entryToValue(entry: EntryType): String =
          entry.entryName
      }

  }

}
