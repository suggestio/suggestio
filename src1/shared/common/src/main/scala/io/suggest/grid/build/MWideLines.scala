package io.suggest.grid.build

import io.suggest.ad.blk.BlockHeight
import japgolly.univeq._

import scala.annotation.tailrec

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.12.17 17:37
  * Description: Модели для представления широких строк в плитке.
  *
  * Модели ориентированы на двух-фазное построение плитки:
  * - Начальная сборка плитки. Собирается плитка без учёта wide-блоков: MWideLines().push().
  * - Новая сборка плитки, но уже с использованием широких блоков: MWideLines().extract()
  */


/** Инфа об одной или нескольких строках, занятых под широкие карточки.
  *
  * @param startLine Номер строки, с которого можно начинать резервирование строк под wide-карточку.
  * @param height Высота (кол-во занятых строк плитки).
  */
case class MWideLine(
                      startLine : Int,
                      height    : BlockHeight
                    ) {

  /** Индекс последней занимаемой строки. */
  def lastLine = nextLine - 1

  /** Номер строки, которая идёт следующей после этого wide-резерва. */
  def nextLine = startLine + height.relSz

  def withStartLine(startLine: Int) = copy(startLine = startLine)

  def range = startLine until nextLine

  /** Узнать, пересекается ли этот отрезок с указанным.
    *
    * @see [[https://stackoverflow.com/a/3269471]]
    */
  def overlaps(other: MWideLine): Boolean = {
    startLine <= other.lastLine &&
      other.startLine <= lastLine
  }

}
object MWideLine {
  implicit def univEq: UnivEq[MWideLine] = UnivEq.derive
}


object MWideLines {

  implicit def univEq: UnivEq[MWideLines] = UnivEq.derive

  private def _assertRecursion(n: Int) = assert(n < 1000)

  /** Запихивание элемента wide-строкового промежутка в сортированный список ему подобных.
    *
    * Сложность до O(2n).
    *
    * @param mwl Добавляем элемент.
    * @param resAccRev Обратный акк результата (пройденных элементов.
    * @param rest Исходные элементы в прямом порядке.
    * @param n Внутренний номер итерации, для защиты от бесконечной рекурсии.
    * @return Новый список элементов в исходном порядке,
    *         и итоговый инстанс [[MWideLine]], который мог быть обновлён.
    */
  @tailrec
  private def _doPush(mwl: MWideLine, resAccRev: List[MWideLine], rest: List[MWideLine], n: Int = 0): (List[MWideLine], MWideLine) = {
    // Явно запретить бесконечную рекурсию.
    _assertRecursion( n )

    rest match {
      case restHd :: restTl =>
        val isNowBeforeRest = mwl.lastLine < restHd.startLine
        //println( isNowBeforeRest, mwl.lastLine, restHd.startLine )
        if (!isNowBeforeRest) {
          // Текущий элемент пересекается с последующим. Без вариантом, перешагиваем через него, обновив mwl
          val mwl2 = mwl.withStartLine( restHd.nextLine )
          _doPush(mwl2, restHd :: resAccRev, restTl, n + 1)

        } else {
          // Есть ещё элементы впереди. Глянуть акк. пройденных элементов:
          val resAccHdOpt = resAccRev.headOption
          val isNowAfterAcc = resAccHdOpt.fold(true) { resAccHd =>
            mwl.startLine > resAccHd.lastLine
          }

          if (isNowAfterAcc) {
            // Текущий элемент прекрасно помещается, не пересекаясь с другими элементами.
            val resRev = (restHd :: mwl :: resAccRev) reverse_::: restTl
            (resRev.reverse, mwl)

          } else {
            // Текущий элемент предшествует текущему началу или пересекается с ним, но находится перед хвостом.
            // Надо снова попытаться запихать сюда, но увеличивив startLine:
            // TODO Opt Инкремент startLine + 1 субоптимален: можно заменять на resAccRev.head.nextLine?
            val mwl2 = mwl.withStartLine( mwl.startLine + 1 )
            _doPush(mwl2, resAccRev, rest, n + 1)
          }
        }

      case _ =>
        // Нет элементов для дальнейшего анализа. Значит просто добавляем текущий узел сюда (в начало rev-списка, т.е. в конец итогового списка).
        val resRev = mwl :: resAccRev
        (resRev.reverse, mwl)
    }
  }


  /** Рекурсивная функция поиска и извлечения подходящего [[MWideLine]] из аккамулятора.
    *
    * @param mwl Описание промежутка широкой строки.
    * @param resAccRev Обратный акк результатов.
    * @param rest Исходный набор строк.
    * @param n Внутренний счётчик итераций рекурсии.
    * @return Результат поиска с обновлённым списком [[MWideLine]] в исходном порядке.
    */
  @tailrec
  private def _doExtract(mwl: MWideLine, resAccRev: List[MWideLine], rest: List[MWideLine], n: Int = 0): Option[(List[MWideLine], MWideLine)] = {
    _assertRecursion( n )

    rest match {
      case restHd :: restTl =>
        // Сначала надо пройти до уровня startLine. Затем извлечь первый попавшийся элемент с эквивалентной height.
        if (restHd.startLine >= mwl.startLine  &&  (mwl.height ==* restHd.height)) {
          // Найден подходящий элемент на подходящем уровне. Его и вернуть.
          val res = resAccRev reverse_::: restTl
          Some((res, restHd))

        } else {
          // Пока ещё не дошли до необходимого уровня. Опускаемся вглубь исходного списка...
          _doExtract(mwl, restHd :: resAccRev, restTl, n + 1)
        }

      case Nil =>
        // should never happen.
        None
    }
  }

}


/** Контейнер-аккамулятор для широких промежутков плитки, описываемыми через [[MWideLine]].
  *
  * @param lines Сортированный аккамулятор инстансов [[MWideLine]].
  */
case class MWideLines(lines: List[MWideLine] = Nil) {

  /** Найти место в аккамуляторе для строки указанной конфигурации,
    * сохранить в аккамулятор, и вернуть итоговую строку.
    */
  def push(mwl: MWideLine): (MWideLines, MWideLine) = {
    // Пройти цикл добавления одного элемента:
    val (lines2, mwl2) = MWideLines._doPush(
      mwl       = mwl,
      resAccRev = Nil,
      rest      = lines
    )

    copy(lines = lines2) -> mwl2
  }


  /** Извлечение данных по строке, с удалением её из исходного аккамулятора lines.
    * Имеет API похожее на push(), чтобы можно было взаимозаменять вызовы: они обратны друг другу.
    */
  def extract(mwl: MWideLine): Option[(MWideLines, MWideLine)] = {
    for {
      (lines2, mwl2) <- MWideLines._doExtract(
        mwl       = mwl,
        resAccRev = Nil,
        rest      = lines
      )
    } yield {
      copy(lines = lines2) -> mwl2
    }
  }


  /** Read-only проверка наличия в аккамуляторе хотя бы одной wide-строки, пересекающейся с указанной.
    *
    * @param wantMwl Желаемый диапазон строк.
    * @return 
    */
  def isBusy(wantMwl: MWideLine): Boolean = {
    lines.exists { mwl =>
      mwl overlaps wantMwl
    }
  }

}
