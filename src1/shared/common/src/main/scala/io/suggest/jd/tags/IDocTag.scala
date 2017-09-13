package io.suggest.jd.tags

import io.suggest.common.coll.Lists
import io.suggest.jd.tags.qd.QdTag
import io.suggest.model.n2.edge.EdgeUid_t
import io.suggest.primo.{IEqualsEq, IHashCodeLazyVal}
import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.reflect.ClassTag

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


  private val _IDT_NAME_FORMAT = (__ \ Fields.TYPE_FN).format[MJdTagName]

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
      case tns.QUILL_DELTA    => _toIdtReads[QdTag]
      case tns.ABS_POS        => _toIdtReads[AbsPos]
      case tns.PLAIN_PAYLOAD  => _toIdtReads[PlainPayload]
      case tns.PICTURE        => _toIdtReads[Picture]
      case tns.STRIP          => _toIdtReads[Strip]
      case tns.DOCUMENT       => _toIdtReads[JsonDocument]
      case _ => ???
    }

    // Собрать в-JSON-рендерер на основе названия тега.
    // TODO Writes указаны явно, потому что компилятор цепляет везде IDOC_TAG_FORMAT вместо нужного типа из-за Writes[-A].
    val w = OWrites[IDocTag] { docTag =>

      // Всякие теги без контента (и без writes) должны возращать null. Остальные -- JsObject.
      val dataJsObjOrNull = docTag match {
        case qd: QdTag            => _writeJsObj(qd)( QdTag.QD_TAG_FORMAT )
        case ap: AbsPos           => _writeJsObj(ap)( AbsPos.ABS_POS_FORMAT )
        case pp: PlainPayload     => _writeJsObj(pp)( PlainPayload.PLAIN_PAYLOAD_FORMAT )
        case p: Picture           => _writeJsObj(p)(  Picture.PICTURE_FORMAT )
        case s: Strip             => _writeJsObj(s)(  Strip.STRIP_FORMAT )
        case d: JsonDocument      => _writeJsObj(d)(  JsonDocument.DOCUMENT_FORMAT )
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



  def batchUpdateOne(source: IDocTag)(batches: JdBatch_t*): Seq[IDocTag] = {
    batchUpdateOne2(source, batches)
  }
  def batchUpdateOne2(source: IDocTag, batches: JdBatches_t): Seq[IDocTag] = {
    // Отработать children
    val children0 = source.children
    val source2 = if (children0.nonEmpty) {
      val children2 = batchUpdate2(children0, batches)
      // Изменилось ли хоть что-то?
      if ( Lists.isElemsEqs(children0, children2) ) {
        // Дочерние элементы не изменились.
        source
      } else {
        // Да, есть изменение -- заливаем новые children внутрь.
        source.withChildren( children2 )
      }
    } else {
      // Нет children -- пропускаем их обработку.
      source
    }
    // Отработать текущий тег.
    // Пройтись по списку batch'ей на предмет совпадения тега.
    batches.foldLeft [Seq[IDocTag]] ( source2 :: Nil ) {
      case (acc0, (fdt, f)) =>
        if (source == fdt) {
          // Применить текущую batch-функцию к аккамулятору.
          f(acc0)
        } else {
          acc0
        }
    }
  }
  def batchUpdate(source: IDocTag*)(batches: JdBatch_t*): Seq[IDocTag] = {
    batchUpdate2(source, batches)
  }
  /** Пакетный апдейт списка исходных тегов с помощью списка функций. */
  def batchUpdate2(sources: Seq[IDocTag], batches: JdBatches_t): Seq[IDocTag] = {
    sources.flatMap { jdt0 =>
      batchUpdateOne2(jdt0, batches)
    }
  }

  /** Типичные Batch-фунцкии и batch'и. */
  object Batches {
    /** Batch-функция удаления тега/тегов. */
    def delete(what: IDocTag): JdBatch_t = replace(what)

    def replaceF(replacements: IDocTag*): JdBatchF_t = {
      {_: Seq[IDocTag] => replacements }
    }
    def replace(what: IDocTag, replacements: IDocTag*): JdBatch_t = {
      what -> replaceF(replacements: _*)
    }
  }

}


/** Интерфейс для всех "тегов" структуры документа.
  *
  * IIdentityFastEq:
  * Теги из дерева используются как ключи в ScalaCSS styleF Map[X,_] прямо во время рендера.
  * Во время тормозных react-рендеров и перерендеров в браузере, ключи должны **очень** быстро работать,
  * поэтому всё оптимизировано по самые уши ценой невозможности сравнивания разных тегов между собой.
  */
trait IDocTag
  extends Product
  // TODO Opt: lazy val: на клиенте желательно val, на сервере - просто дефолт (def). Что тут делать, elidable нужен какой-то?
  with IHashCodeLazyVal
  with IEqualsEq
{

  /** Название тега. */
  def jdTagName: MJdTagName

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

  def deepEdgesUidsIter: Iterator[EdgeUid_t] = {
    deepChildrenIter.flatMap(_.deepEdgesUidsIter)
  }

  def deepChildrenOfTypeIter[T <: IDocTag : ClassTag]: Iterator[T] = {
    deepChildrenIter
      .flatMap {
        case t: T => t :: Nil
        case _ => Nil
      }
  }

  def deepOfTypeIter[T <: IDocTag : ClassTag]: Iterator[T] = {
    val chIter = deepChildrenOfTypeIter[T]
    this match {
      case t: T => Iterator(t) ++ chIter
      case _    => chIter
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


  /** Прооптимизировать текущее дерево. */
  def shrink: Seq[IDocTag] = {
    val children0 = children
    val this2 = if (children.nonEmpty) {
      val children2 = children.flatMap(_.shrink)
      if ( Lists.isElemsEqs(children0, children2) ) {
        this
      } else {
        withChildren( children2 )
      }
    } else {
      this
    }
    this2 :: Nil
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
    val _children = children
    if (_children.isEmpty) {
      this :: Nil

    } else {
      val this2 = if (_children contains what) {
        // Обновление элемента на текущем уровне
        val children2 = _children.flatMap { jdt =>
          if (jdt == what) {
            updated
          } else {
            jdt :: Nil
          }
        }
        withChildren(children2)

      } else {
        // Обновление элементов где-то на подуровнях дочерних элементов.
        val children2 = _children.flatMap { jdt =>
          jdt.deepUpdateChild(what, updated)
        }
        // Возможно, что ничего не изменилось. И тогда можно возвращать исходный элемент вместо пересобранного инстанса.
        if ( Lists.isElemsEqs(_children, children2) ) {
          this
        } else {
          withChildren( children2 )
        }
      }

      this2 :: Nil
    }
  }


  /** Вернуть инстанс текущего тега с обновлённым набором children'ов. */
  def withChildren(children: Seq[IDocTag]): IDocTag = {
    throw new UnsupportedOperationException
  }


  /** Глубинный flatMap(): на каждом уровне сначала отрабатываются дочерние элементы, затем родительский. */
  def flatMap(f: IDocTag => Seq[IDocTag]): Seq[IDocTag] = {
    val this2 = if (children.isEmpty) {
      this
    } else {
      withChildren(
        children.flatMap { ch =>
          ch.flatMap(f)
        }
      )
    }
    f(this2)
  }


  def map(f: IDocTag => IDocTag): IDocTag = {
    val this2 = if (children.isEmpty) {
      this
    } else {
      withChildren(
        for (ch <- children) yield {
          ch.map(f)
        }
      )
    }
    f(this2)
  }


  def foreach[U](f: IDocTag => U): Unit = {
    for (ch <- children) {
      ch.foreach(f)
    }
    f(this)
  }


  def contains(jdt: IDocTag): Boolean = {
    deepChildrenIter.contains( jdt )
  }

}
