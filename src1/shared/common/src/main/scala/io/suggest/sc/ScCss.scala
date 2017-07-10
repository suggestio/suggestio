package io.suggest.sc

import io.suggest.css.Css

import ScScalaCssDefaults._
import io.suggest.i18n.MsgCodes

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 15:01
  * Description: scalaCSS для выдачи s.io.
  */
object ScCss extends StyleSheet.Inline {

  import dsl._

  /** Строковые обозначения стилей наподобии i.s.css.Css. */
  private def __ = Css.__
  private def _NAME_TOKENS_DELIM = "-"
  private def _SM_ = Css.Lk._SM_PREFIX_

  private def _styleAddClasses(cssClasses: String*) = style( addClassNames(cssClasses: _*) )

  private def `BUTTON` = "button"
  private def _SM_BUTTON = _SM_ + `BUTTON`

  //val button = _styleAddClasses( _SM_BUTTON )

  /** Стили для html.body . */
  // TODO Этот код наверное не нужен. Т.к. оно вне react-компонента.
  object Body {

    /** Главный обязательный статический класс для body. */
    val smBody = _styleAddClasses( _SM_ + "body" )

    /** Фоновый SVG-логотип ЯПРЕДЛАГАЮ. Его в теории может и не быть, поэтому оно отдельно от класса body. */
    object BgLogo {
      val ru = _styleAddClasses( __ + "ru" )
      val en = _styleAddClasses( __ + "en" )
    }

    /** Исторически как-то сложилось, что активация body происходит через style-аттрибут. Но это плевать наверое. */
    val smBodyReady = style(
      overflow.hidden,
      backgroundColor.white
    )

  }


  /** Стили для #sioMartRoot и каких-то вложенных контейнеров */
  object Root {

    /** корневой div-контейнер. */
    val root = _styleAddClasses( _SM_ + "showcase" )

  }


  /** Стили для заголовка. */
  object Header {

    /** Корневое имя класса, от которого идёт остальное словообразование. */
    private def HEADER = _SM_ + "producer-header"

    /** Стили контейнера любого заголовка. */
    val header = _styleAddClasses( HEADER, Css.Position.ABSOLUTE )

    object Buttons {

      /** Начальный список для сборки стилей разных кнопок на панели заголовка. */
      private val _allBtnStyles: List[String] = {
        _SM_BUTTON ::
          (HEADER + "_btn") ::
          Css.Position.ABSOLUTE ::
          Nil
      }


      /** Выравнивание кнопок. */
      object Align {
        private def __aligned(where: String) = __ + where + "-aligned"
        def LEFT  = __aligned( MsgCodes.`left` )
        def RIGHT = __aligned( MsgCodes.`right` )
      }

      //val leftGeoBtn = _styleAddClasses( _btnMx, HEADER + "_geo-button", Align.LEFT )

      private def _btnClass(root: String) = HEADER + "_" + root + "-" + `BUTTON`

      /** Стиль кнопки поиска. */
      val search = _styleAddClasses(
        _btnClass("search") :: Align.RIGHT :: _allBtnStyles: _*
      )

      /** Стиль кнопки меню слева. */
      val menu = _styleAddClasses(
        _btnClass("geo") :: Align.LEFT :: _allBtnStyles: _*
      )

      /** Стиль кнопки заголовка, который указывает вправо. */
      val right = _styleAddClasses(
        Align.RIGHT :: _allBtnStyles: _*
      )

      /** Стиль кнопки заголовка, которая указывает влево. */
      val left = _styleAddClasses(
        Align.LEFT :: _allBtnStyles: _*
      )

    }


    /** Доступ к стилям логотипа узла. */
    object Logo {

      /** Суффикс названия css-классов разных логотипов. */
      private def `-logo` = "-logo"

      /** Стили текстового логотипа узла.*/
      object Txt {

        private def TXT_LOGO = HEADER + "_txt" + `-logo`
        val txtLogo = _styleAddClasses( TXT_LOGO )

        /** Точки по краям названия узла. */
        object Dots {
          private val DOT = TXT_LOGO + "-dot"
          val left = _styleAddClasses( DOT, __ + MsgCodes.`left` )
          val right = _styleAddClasses( DOT, __ + MsgCodes.`right` )
        }

      }

    }

  }

}
