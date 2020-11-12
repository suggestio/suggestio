package io.suggest.svg

import java.awt.geom.RectangularShape
import java.io.{File, FileInputStream, InputStream}

import io.suggest.common.geom.d2.MSize2di
import io.suggest.util.logs.MacroLogsImpl
import org.apache.batik.anim.dom.SAXSVGDocumentFactory
import org.apache.batik.bridge.{BridgeContext, DocumentLoader, GVTBuilder, UserAgentAdapter}
import org.apache.batik.gvt.GraphicsNode
import org.apache.batik.util.XMLResourceDescriptor
import org.w3c.dom.{Document, Node}

import scala.annotation.tailrec
import scala.util.Try
import scala.concurrent.blocking

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.09.14 11:45
 * Description: Утиль для работы с svg.
 */
object SvgUtil extends MacroLogsImpl {

  /** Для рассчёта целочисленного размера сторон SVG-файла, необходимо подтягивать размеры в область
    * натуральных числе.
    * SVG-файл может иметь координаты в дробях где-то между 0 и 1, при округлении возвращая нули.
    *
    * @param rect2d Прямоугольник AWT.
    * @return Размерчик.
    */
  def rect2Size2d(rect2d: RectangularShape): MSize2di = {
    // Уровень в пикселях, ниже которого размер не должен быть.
    // Выведенные размеры должны позволять оценивать пропорцию картинки с какой-то необходимой точностью.
    val magnifyBy = 10
    val minSideSizePx = magnifyBy

    @tailrec def __magnifySizes(
                                 w0: Double = rect2d.getWidth,
                                 h0: Double = rect2d.getHeight,
                                 restStepCount: Int = 7,
                               ): MSize2di = {
      if (
        (restStepCount > 0) && (
          // Если оба размера одновременно маловаты
          ((w0 < minSideSizePx) && (h0 < minSideSizePx)) ||
          // или если есть хотя бы один размер меньше 1 (нельзя же нулевой размер в базу записывать, даже если второй размер ненулевой)
          (w0 < 1) || (h0 < 1)
      )
    ) {
        __magnifySizes(
          w0 = w0 * magnifyBy,
          h0 = h0 * magnifyBy,
          restStepCount - 1,
        )
      } else {
        MSize2di( width = w0.toInt, height = h0.toInt )
      }
    }

    __magnifySizes()
  }

  /** Инстанс или сборка инстансов document factory. */
  private val documentFactory = {
    val parser = XMLResourceDescriptor.getXMLParserClassName
    new SAXSVGDocumentFactory( parser )
  }


  /** Приведение файла к ссылки, которая требуется для открытия svg-файла как документа.
    *
    * @param f SVG-файл, который планируется открывать.
    * @return Строка-ссылка.
    */
  def file2svgDocUrl(f: File): String = {
    f .toURI
      .toURL
      .toString
  }

  /** Открыть SVG-документ для дальнейшего использования в API этого модуля.
    *
    * @param f Открываемый файл.
    * @return Документ | null | exception.
    */
  def open(f: File): Document = {
    val docUrl = file2svgDocUrl(f)

    blocking {
      val is = new FileInputStream(f)
      try {
        open(is, docUrl)
      } finally {
        is.close()
      }
    }
  }

  /** Открыть SVG-документ на основе input stream и какой-то ссылки.
    *
    * @param is InputStream.
    * @param url Ссылка на svg-документ, которая может как-то влиять на обработку или рендер.
    * @return Документ | null | exception.
    */
  def open(is: InputStream, url: String): Document = {
    documentFactory.createDocument(url, is)
  }


  /** Безопасная обёртка над вызовом open().
    * Подходит как ПОВЕРХНОСТНАЯ проверка валидности документа.
    *
    * Использовать надо так:
    * isSvgValid2( open(...) ) => Option[Document]
    *
    * @param open Функция открытия документа, которая может вернуть экзепшен, null или даже документ.
    * @return Опционально: открытый документ, готовый к использованию.
    *         None, если документ пуст, невалиден или отсутствует.
    */
  def safeOpenWrap(open: => Document, errorMsg: => String): Option[Document] = {
    def logPrefix = s"safeOpenWrap()#${System.currentTimeMillis()}:"
    val tryDoc = Try(open)

    for (ex <- tryDoc.failed)
      LOGGER.warn(s"$logPrefix Invalid SVG data $errorMsg", ex)

    val docOpt = tryDoc
      .toOption
      .flatMap( Option.apply )

    if (docOpt.isEmpty && tryDoc.isSuccess)
      LOGGER.warn(s"$logPrefix doc is empty $errorMsg")

    docOpt
  }

  /** Рендер SVG-документа в GVT-дерево.
    * Это во многом аналогично нормальному рендеру.
    * С помощью этого метода можно проверить валидность документа, и даже измерить svg-область:
    * {{{
    *   val gvt = buildGvt(...)
    *   val bounds = gvt.getBounds()
    *
    *   bounds.getWidth()
    *   bounds.getHeight()
    *
    *   import io.suggest.common.geom.d2.MSize2diJvm.Implicits._
    *   bounds.toSize2di
    * }}}
    *
    * @param doc SVG-документ.
    * @return Корень GVT-дерева.
    */
  def buildGvt(doc: Document): GraphicsNode = {
    try {
      val agent = new UserAgentAdapter
      val loader = new DocumentLoader(agent)
      val bridgeContext = new BridgeContext(agent, loader)
      bridgeContext.setDynamic(true)

      val builder = new GVTBuilder()
      builder.build(bridgeContext, doc)
    } catch {
      case ex: Error =>
        throw new IllegalArgumentException("Batik deeply failed", ex)
    }
  }


  /** Попытаться прочитать из полей документа данные о фактическом размере картинки.
    *
    * @param doc Документ.
    * @return Размер, если задан.
    */
  def getDocWh(doc: Document): Option[MSize2di] = {
    Try {
      for {
        svgRoot     <- Option( doc.getFirstChild )
        attrs       <- Option( svgRoot.getAttributes )
        widthAv     <- Option( attrs.getNamedItem("width") )
        __parseAv = (av: Node) => Math.round( av.getNodeValue.toDouble ).toInt
        widthPx = __parseAv( widthAv )
        if {
          val r = widthPx > 0
          if (!r) LOGGER.warn(s"Invalid width: $widthPx px ($widthAv)")
          r
        }
        heightAv    <- Option( attrs.getNamedItem("height") )
        heightPx = __parseAv( heightAv )
        if {
          val r = heightPx > 0
          if (!r) LOGGER.warn(s"Invalid height: $widthPx px ($widthAv)")
          r
        }
      } yield {
        MSize2di(
          width  = widthPx,
          height = heightPx,
        )
      }
    }
      .toOption
      .flatten
  }

  /**
   * Может ли указанные mime-тип быть svg? Это нужно для исправления JMimeMagic.
   * @param mime MIME-type.
   * @return true, если svg внутри допустим. Иначе false.
   */
  def maybeSvgMime(mime: String): Boolean = {
    mime.startsWith("text/") || mime.startsWith("image/svg")
    // TODO Заюзать MImgFmts.SVG.mimePrefix для "image/svg"
  }

}
