package io.suggest.ym.cat

import com.github.tototoshi.csv.CSVReader
import java.io.{FileNotFoundException, InputStreamReader}
import scala.collection.mutable
import scala.annotation.tailrec
import io.suggest.util.{TextUtil, Lists}
import io.suggest.ym.{NormTokensOutAn, YmStringsAnalyzer}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.01.14 13:21
 * Description: Поддержка категорий из дерева категорий яндекс-маркета.
 * У этого компонента есть несколько задач:
 * - Описывать базовое дерево всех категорий товаров.
 */

object YmCategory {

  /** Разделитель категорий в пути market_category. */
  val CAT_PATH_SEP = "/"

  /** Тип рекурсирвного дерева категорий. */
  type CatTreeMap_t = Map[String, YmCategory]
  private type MutCatTreeMap_t = mutable.HashMap[String, YmMutCategory]

  /** Промежуточный класс, используемый в конструкторе при аккумулировании категорий. */
  private case class YmMutCategory(name: String, subcats: MutCatTreeMap_t, level:Int)

  /** Скрытый метод, вызываемый в конструкторе, изменяющий карту-аккамулятор, чтобы указанная категория там точно была. */
  @tailrec private def ensureCatPath(path: List[String], mapAcc1: MutCatTreeMap_t, currLevel:Int) {
    if (!path.isEmpty) {
      val h = path.head
      val subLevel = currLevel + 1
      val sub = mapAcc1.getOrElseUpdate(h, new YmMutCategory(h, new mutable.HashMap, subLevel))
      ensureCatPath(path.tail, sub.subcats, subLevel)
    }
  }

  /** Перегонка аккумулятора дерева в неизменяемое состояние. Вызывается в конструкторе. */
  private def compileMutCatTree(mct: MutCatTreeMap_t, parentPathRev: List[String]): CatTreeMap_t = {
    mct.map { case (k, v) =>
      val subcatsOpt = if (v.subcats.isEmpty) {
        None
      } else {
        val result = compileMutCatTree(v.subcats, k :: parentPathRev)
        Some(result)
      }
      k -> YmCategory(k, subcatsOpt, v.level, parentPathRev)
    }.toMap
  }


  /** Нужно распарсить в память csv-файл с категориями и загрузить его в память в виде immutable-дерева. */
  val CAT_TREE: YmCategoryRoot = {
    val rscName = "ym/cat/market_category.csv"
    val is = getClass.getClassLoader.getResourceAsStream(rscName)
    if (is == null) {
      throw new FileNotFoundException(s"Cannot open resource $rscName - Not found.")
    }
    val reader = new InputStreamReader(is, "UTF-8")
    try {
      val csvReader = CSVReader.open(reader)
      val it = csvReader.iterator
      if (!it.hasNext) {
        throw new Exception(s"Resource $rscName is empty!")
      }
      val mapAcc: MutCatTreeMap_t = new mutable.HashMap
      csvReader.iterator.foreach { l =>
        // Итератор возвращает List(Товары для детей, Для школы, Чертежные принадлежности, "", "")
        // Т.е. всего 5 уровней максимум. И часто бывает, что нижние категории пути пустые.
        val catPath = l.map(_.trim).filter(!_.isEmpty).toList
        ensureCatPath(catPath, mapAcc, currLevel=0)
      }
      // mutable-карта не является результатом. Нужно сконвертить эту карту в immutable-представление и без мусора на концах.
      val resultTree = compileMutCatTree(mapAcc, Nil)
      YmCategoryRoot(resultTree)

    } finally {
      reader.close()
    }
  }


  /** Поиск общего пути между несколькими списками списками.
    * @param cats Список категорий, между которыми вычисляется общая надкатегория.
    * @return Общий хвост, или Nil если общего хвоста нет.
    */
  def getCommonPathRev(cats: YmCategoryT*): List[String] = {
    if (cats.isEmpty) {
      throw new IllegalArgumentException("No categories passed as arguments.")
    }
    val h :: t = cats
    // Перебор всех переданных категорий для нахождения общего хвоста.
    val (_, commonPath) = t.foldLeft (h.level -> h.parentPathRev) { case (acc @ (commonLevel, commonPathRev), e) =>
      // Сначала надо уравнять длины хвостов, чтобы потом по ним проходить и сравнивать.
      val eSubPath = Lists.nthTail(e.parentPathRev, commonLevel)
      val newTail = Lists.getCommonTail(commonPathRev, eSubPath)
      if (newTail eq commonPathRev)  acc  else  newTail.size -> newTail
    }
    commonPath
  }


  /** Внутренний тип trgm-аккамулятора, используемый только в конструкторе. */
  private type MutCatTrgmAcc_t = mutable.HashMap[String, List[YmCategoryT]]
  /** Анализатор, используемый для нормализации категорий при их индексации и поиске. */
  def getAnalyzer = new YmStringsAnalyzer

  /** Загрузить дерево категорий в изменяемый словарь-аккамулятор TRGM-токенов. */
  private def loadCatTreeTrgm(ctm: CatTreeMap_t, acc: MutCatTrgmAcc_t, parentTrgm: Set[String])(implicit an: NormTokensOutAn) {
    ctm.foreach { case (_, cat) =>
      val nameTokens = an.toNormTokensRev(cat.name)
      val trgm = getTrgmSet(nameTokens)
      // Залить все trgm-токены в аккамулятор словаря.
      trgm foreach { trgm =>
        val v1 = acc.get(trgm) match {
          case Some(otherCats) => cat :: otherCats
          case None            => List(cat)
        }
        acc(trgm) = v1
      }
      // Также, пройти все подкатегории, пробросив туда текущий словарь trgm-токенов.
      if (cat.subcatsOpt.isDefined) {
        loadCatTreeTrgm(cat.subcatsOpt.get, acc, trgm)
      }
    }
  }

  /** immutable-словарь триграмм, указывающий на категории, в которых встречается указанная последовательность.
    * Используется для определения категории, которую имел в виду юзер в своей писанине.
    * Lucene-индекс не используется, т.к. он не обладает необходимым уровне потоко-безопасности, т.к.
    * ориентирован на большие документарные коллекции, а не на поиск среди кучки классов и строчек определённого формата. */
  val CAT_TRGM_MAP: Map[String, Set[YmCategoryT]] = {
    // Для нормализации писанины используем обычный анализатор, используемый в других Ym-парсерах.
    implicit val an = getAnalyzer
    val acc: MutCatTrgmAcc_t = mutable.HashMap()
    loadCatTreeTrgm(CAT_TREE.cats, acc, Set.empty)
    // Сделать из изменяемого аккумулятора нормальную потоко-безопасную одномерную карту.
    acc.mapValues(_.toSet).toMap
  }


  /** Используемый метод генерации триграмм. Используем full trgm, т.к. начала категорий очень многое значат.
    * Метод используется как на стадии подготовки словаря, так и при поиске, поэтому вынесен сюда.
    * @param token Исходная строка. Обычно одно слово из имени.
    * @return Список триграмм в неопределённом порядке.
    */
  def getTrgmList(token: String): List[String] = TextUtil.trgmTokenFull(token)
  def getTrgmSet(token: String) = getTrgmList(token).toSet
  def getTrgmSet(tokens: Seq[String], acc0: Set[String] = Set.empty): Set[String] = {
    tokens.foldLeft(acc0) {
      (acc, token) => acc ++ getTrgmList(token)
    }
  }

}


import YmCategory._

sealed trait YmCategoryT {
  val subcatsOpt: Option[CatTreeMap_t]
  val level: Int
  def parentPathRev: List[String]

  def /(subcat: String): YmCategory = subcatsOpt.get(subcat)
  def contains(subcat: String): Boolean
  def pathRev: List[String]
  def path: List[String] = pathRev.reverse
  val pathStr: String
  def isRealCategory: Boolean

  def getCommonPathRevWith(e: YmCategoryT) = getCommonPathRev(this, e)
  def topLevelCatName: String
  def topLevelCat = CAT_TREE / topLevelCatName
}

/**
 * Класс, описывающий одну категорию любого уровня и поддерево её подкатегорий.
 * @param name Название текущей категории.
 * @param subcatsOpt Карта-дерево подкатегорий, если есть.
 */
sealed case class YmCategory(name: String, subcatsOpt: Option[CatTreeMap_t], level:Int, parentPathRev:List[String]) extends YmCategoryT {
  def contains(subcat: String) = subcatsOpt exists { _ contains subcat }
  val pathRev = name :: parentPathRev
  val pathStr = path mkString "/"
  def isRealCategory = true
  def topLevelCatName = pathRev.last
  override def toString = s"${getClass.getSimpleName}($name)"
}

/**
 * Верхний элемент дерева категорий, безымянная мета-категория нулевого уровня.
 * Нет категории верхнего уровня, поэтому используется специальный объект для хранения такого добра.
 * @param cats Дерево категорий.
 */
sealed case class YmCategoryRoot(cats: CatTreeMap_t) extends YmCategoryT {
  val subcatsOpt = Some(cats)
  def contains(subcat: String) = cats contains subcat
  val level = 0
  def parentPathRev = Nil
  def pathRev = parentPathRev
  val pathStr = ""
  def isRealCategory = false
  def topLevelCatName: String = ???
}

