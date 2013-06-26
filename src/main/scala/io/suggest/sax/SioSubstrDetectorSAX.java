package io.suggest.sax;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.06.13 9:33
 * Description: SAX для определения наличия в текстах указанной подстроки. Используется для валидации доменов через текстовые файлы
 * без привязки к формату.
 * Реализован на жабе, ибо на scala нет возможности реализовать этот класс эффективно.
 */
public class SioSubstrDetectorSAX extends DefaultHandler {

    protected final char[] _wantedChars;

    // Курсор используется для позиционирования в искомом массиве символов между вызовами characters().
    protected int _cursor = 0;

    // Кол-во найденных вхождений указанной строки в потоке.
    protected int _matchesFound = 0;

    /**
     * Основной конструктор.
     * @param wantedChars Искомая последовательность символов.
     */
    public SioSubstrDetectorSAX(char[] wantedChars) {
        _wantedChars = wantedChars;
    }

    /**
     * Вспомогательный конструктор. Собирает экземпляр класса на основе строки.
     * @param str Искомая строка
     */
    public SioSubstrDetectorSAX(String str) {
        this(str.toCharArray());
    }

    /**
     * Парсер обнаружил текст. Нужно поискать в наборе символов искомую подстроку.
     * @param ch Массив данных парсера
     * @param start Начало набора символов
     * @param length Длина последовательности символов.
     * @throws SAXException
     */
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        for(int i = start; i < start + length; i++) {
            if (ch[i] == _wantedChars[_cursor]) {
                _cursor++;
                // Если пройдены все искомые символы, то значит найдено совпадение в тексте.
                if(_cursor >= _wantedChars.length) {
                    _matchesFound++;
                    _cursor = 0;
                }
            } else {
                // Текущий символ не подходит. Сбросить счетчик искомой последовательности на начало.
                if(_cursor > 0)
                    _cursor = 0;
            }
        }
    }


    /**
     * Выдачить количество найденных попаданий.
     * @return Неотрицательное целое.
     */
    public int getMatchesFound() {
        return _matchesFound;
    }

    /**
     * Есть ли вообще совпадения?
     * @return true, если счетчик совпадений больше нуля. Иначе, false.
     */
    public boolean isMatchFound() {
        return _matchesFound > 0;
    }
}
