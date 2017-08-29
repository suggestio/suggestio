package io.suggest.jd.tags

import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.runtime.ScalaRunTime

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.08.17 12:19
  * Description: Интерфейс каждого элемента структуры документа.
  * Структура аналогична html/xml-тегам, но завязана на JSON и названа структурой, чтобы не путаться.
  */
object IDocTag {

  object Fields {

    val TYPE_FN = "t"

    /** Имя поля с дочерними элементами. По идее -- оно одно на все реализации. */
    val CHILDREN_FN = "c"

  }


  private val _IDT_NAME_FORMAT = (__ \ Fields.TYPE_FN).format[MDtName]

  /** Приведение Reads[Реализация IDocTag] к Reads[IDocTag]. Компилятор не хочет этого делать сам. */
  private def _toIdtReads[X <: IDocTag](implicit rx: Reads[X]): Reads[IDocTag] = {
    // TODO .map(d => d: IDocTag) - это небесплатный костыль в коде. Разруливаем через asInstanceOf[], что суть есть zero-cost костыле-хак. Надо как-то нормально это разрулить.
    rx.asInstanceOf[Reads[IDocTag]] //.map(d => d: IDocTag)
  }

  /** Отрендерить в JsObject. Это аналог Json.writes[X].writes(x) */
  private def _writeJsObj[X <: IDocTag](x: X)(implicit ow: OWrites[X]): JsObject = {
    ow.writes(x)
  }

  /** Полиморфная поддержка play-json. */
  implicit val IDOC_TAG_FORMAT: OFormat[IDocTag] = {
    // Собрать читалку на основе прочитанного имени тега.
    val tns = MJdTagNames
    val r: Reads[IDocTag] = _IDT_NAME_FORMAT.flatMap[IDocTag] {
      case tns.PLAIN_PAYLOAD  => _toIdtReads[PlainPayload]
      case tns.LINE_BREAK     => Reads.pure( LineBreak )
      case tns.TEXT           => _toIdtReads[Text]
      case tns.PICTURE        => _toIdtReads[Picture]
      case tns.ABS_POS        => _toIdtReads[AbsPos]
      case tns.STRIP          => _toIdtReads[Strip]
      case tns.DOCUMENT       => _toIdtReads[JsonDocument]
      case _ => ???
    }

    // Собрать в-JSON-рендерер на основе названия тега.
    // TODO Writes указаны явно, потому что компилятор цепляет везде IDOC_TAG_FORMAT вместо нужного типа из-за Writes[-A].
    val w = OWrites[IDocTag] { docTag =>

      // Всякие теги без контента (и без writes) должны возращать null. Остальные -- JsObject.
      val dataJsObjOrNull = docTag match {
        case pp: PlainPayload     => _writeJsObj(pp)(PlainPayload.PLAIN_PAYLOAD_FORMAT)
        case LineBreak            => null
        case t: Text              => _writeJsObj(t)(Text.TEXT_FORMAT)
        case p: Picture           => _writeJsObj(p)(Picture.PICTURE_FORMAT)
        case ap: AbsPos           => _writeJsObj(ap)(AbsPos.ABS_POS_FORMAT)
        case s: Strip             => _writeJsObj(s)(Strip.STRIP_FORMAT)
        case d: JsonDocument      => _writeJsObj(d)(JsonDocument.DOCUMENT_FORMAT)
        case _ => ???
      }

      val jdTagNameObj = _IDT_NAME_FORMAT.writes( docTag.jdTagName )

      // Добавить информацию по типу в уже отрендеренный JSON.
      Option( dataJsObjOrNull )
        // Бывают теги без контента, для них надо просто вернуть объект с их типом.
        .fold( jdTagNameObj ) { dataJsObj =>
          val jdTagNameField = jdTagNameObj.value.head
          dataJsObj.copy(
            underlying = dataJsObj.value + jdTagNameField
          )
        }
    }

    OFormat(r, w)
  }


  /** Поддержка play-json для поля children. */
  val CHILDREN_IDOC_TAG_FORMAT: OFormat[Seq[IDocTag]] = {
    (__ \ Fields.CHILDREN_FN).lazyFormatNullable( implicitly[Format[Seq[IDocTag]]] )
      .inmap[Seq[IDocTag]](
        {tagsOpt =>
          tagsOpt.fold[Seq[IDocTag]](Nil)(identity)
        },
        {tags =>
          if (tags.isEmpty) None else Some(tags)
        }
      )
  }


  implicit def univEq: UnivEq[IDocTag] = UnivEq.force

}


/** Интерфейс для всех "тегов" структуры документа. */
trait IDocTag extends Product {

  /** Название тега. */
  def jdTagName: MDtName

  /** Дочерние теги. */
  def children: Seq[IDocTag]


  /** Итератор текущего элемента и всех его под-элементов со всех под-уровней. */
  def deepIter: Iterator[IDocTag] = {
    Iterator(this) ++ deepChildrenIter
  }

  /** Итератор всех дочерних элементов со всех под-уровней. */
  def deepChildrenIter: Iterator[IDocTag] = {
    children.iterator
      .flatMap { _.deepIter }
  }


  // Теги из дерева используются как ключи в Map[X,_] прямо во время рендера.
  // Во время тормозных react-рендеров и перерендеров в браузере, ключи должны **очень** быстро работать,
  // поэтому всё оптимизировано по самые уши.

  /**
    * Реализация хеширования, когда операций сравнения на повтоных вызовах сведено к O(1).
    * Это надо для быстрого рендера, который зависит от Map[IDocTag,_] (внутри scalaCSS Domain).
    */
  // TODO Opt: lazy val: на клиенте желательно val, на сервере - просто дефолт (def). Что тут делать, elidable нужен какой-то?
  override final lazy val hashCode = ScalaRunTime._hashCode(this)

  /** Сравнивание по указателям, т.е. O(1).
    * Это чрезвычайно суровое решение, но так надо, чтобы подружить scalaCSS Domains и рендеринг. */
  override final def equals(obj: Any): Boolean = {
    obj match {
      case idt: IDocTag => idt eq this
      case _ => false
    }
  }

  /** Найти в дереве указанный тег в дереве и обновить его с помощью функции. */
  def deepUpdateOne[T <: IDocTag](what: T, updated: Seq[IDocTag]): Seq[IDocTag] = {
    // Обновляем текущий тег
    if (this == what) {
      updated
    } else {
      // Попробовать пообнавлять children'ов.
      deepUpdateChild( what, updated )
    }
  }


  /** Найти в дереве указанный тег в дочерних поддеревьях и обновить его с помощью функции.
    * Поиск в дереве идёт исходят из того, что элемент там есть, и он должен быть найден
    * как можно ближе к корню дерева. Поэтому сначала обрабатывается полностью над-уровень, и
    * только если там ничего не найдено, то происходит рекурсивное погружение на следующий уровень.
    *
    * Если вдруг одинаковый инстанс тега встречается несколько раз на разных уровнях,
    * то будет обновлён только наиболее верхний уровень с найденными тегами. Но это считается
    * вообще ненормальной и неправильной ситуацией, поэтому не следует использовать boopickle
    * для редактируемых json-документов.
    *
    * @param what Инстанс искомого тега.
    * @param updated Функция обновления дерева.
    * @tparam T Тип искомого тега.
    * @return Обновлённое дерево.
    */
  def deepUpdateChild[T <: IDocTag](what: T, updated: Seq[IDocTag]): Seq[IDocTag] = {
    if (children.isEmpty) {
      this :: Nil

    } else {
      val children2 = if (children.contains(what)) {
        // Обновление элемента на текущем уровне
        children.flatMap { jdt =>
          if (jdt == what) {
            updated
          } else {
            jdt :: Nil
          }
        }

      } else {
        // Обновление элементов где-то на подуровнях дочерних элементов.
        children.flatMap { jdt =>
          jdt.deepUpdateChild(what, updated)
        }
      }
      withChildren(children2) :: Nil
    }
  }


  /** Вернуть инстанс текущего тега с обновлённым набором children'ов. */
  def withChildren(children: Seq[IDocTag]): IDocTag = {
    throw new UnsupportedOperationException
  }

}
