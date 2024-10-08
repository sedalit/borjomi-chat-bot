// Инициализируем переменные, также, если канал - чатвиджет, парсим utm метку
function initVariables() {
    $.session.channel = $.request.channelType;
    integration.init();
    $.session.paginationPage = {
        "discounts": 0,
        "catalog": 0,
        "sections": 0,
        "items": 0,
    };
    if (getChannelType('chatwidget')) parseChatWidgetData();
}

// Функция парсинга данных, пришедших в бота из браузера
function parseChatWidgetData() {
    if ($.parseTree.text.length > "/start".length) {
        var startData = JSON.parse($.parseTree.text.substr("/start".length + 1));
        if (startData && startData.hasOwnProperty('utmId')) {
            $.client.utmId = startData['utmId'];
        }
    }
}

function transferToOperator(response) {
    response.replies = response.replies || [];
    response.replies.push({
        type:"switch",
        appendCloseChatButton: true,
        closeChatPhrases: ["/close", "Закрыть диалог"],
        sendMessagesToOperator: true,
        attributes: $.session.transferData || {}
    });
}

// Функция очистки текста запроса от всего, кроме цифр. Пока что используется только в валидации телефонного номера
function clearNoDigits(query) {
    var numbers = query.replace(/[^\d]/g, '');
    return numbers;
}

function clearSymbols(query) {
    query = query.replace( /[\s.;,?%-\\]/g, '');
    query = query.toLowerCase();
    return query;
}

// Функция валидации введённого пользователем номера телефона для последующей отправки этого номера в апи
function validatePhone(query) {
    var numbers = clearNoDigits(query);
    if (numbers.length > 10) numbers = numbers.slice(-10);
    return numbers;
}

// Функция счётчика, используется для подсчёта попаданий в noMatch
function counter(key) {
    $.session.counter = $.session.counter || {};
    $.session.counter[key] = $.session.counter[key] || 0;
    $.session.counter[key]++;
    return $.session.counter[key];
}

// Функция получения/сравнивания текущего канала с аргументом. Если передаётся аргумент, возвращает значение, является ли аргумент текущим каналом. В ином случае возвращает название текущего канала
function getChannelType(channel) {
    return channel ? $.session.channel === channel : $.session.channel;
}

// Обёртка для работы массивом пришедших от апи элементов
var catalog = new function() {
    // Функция создания массива элементов для постраничной навигации, в котором правила наполнения определяются текущим каналом.
    // Возвращает двумерный массив, в котором ключ - страница в пагинации, а значение - массив элементов для этой страницы.
    this.create = function(items) {
        if (getChannelType("whatsapp")) {
            return this.forWhatsapp(items);
        } else {
            return this.forTelegram(items);
        }
    }
    
    // Создаём массив элементов для пагинации в канале WhatsApp, в котором мы должны показывать 2 элемента на первой странице, и 1 элемент на всех последующих страницах. 
    this.forWhatsapp = function(items) {
        var catalog = [[]]
        var i = 0; //счетчик страниц
        items.forEach(function(item) {
            if (catalog[0].length > 1) {
                i++;
                catalog[i] = [];
            }
            catalog[i].push(item);
        });
        return catalog;
    }
    
    // Аналогично тому, что выше, с той разницей, что здесь мы кладём по 2 элемента на страницу
    this.forTelegram = function(items) {
        var catalog = [[]];
        var i = 0; //счетчик страниц
        items.forEach(function(item) {
            if (catalog[i].length === 2) {
                i++;
                catalog[i] = [];
            }
            catalog[i].push(item);
        });
        return catalog;
    }
    
    this.oneItemPerPage = function(items) {
        var catalog = [];
        var i = 0;
        items.forEach(function(item) {
           catalog[i] = []
           catalog[i].push(item);
           i++;
        });
        return catalog;
    }
}

function answerLastOrderCatalog(orders) {
    var buttons = [];
    var text = "";
    orders.forEach(function(order, index) {
        text += "Товар " + (index + 1) + ":" +
            '\n' + order.Name +
            '\n' + order.UnitCount + " " + order.UnitName +
            '\n' + removeTags(order.Desc) +
            '\n\n' + order.Price;
        buttons.push({"text": index + 1, "transition": "/SpecificItem"})
    })
    
    buttons.push({"text": "В каталог", "transition": "/Catalog"})
    
    $reactions.answer(text);
    $reactions.buttons(buttons);
}

function findItemsInCatalog(catalog, itemsToFind) {
    var toReturn = [];
    
    var sections = _.chain(catalog)
        .pluck('Sections')
        .flatten()
        .value();

    var items = _.chain(sections)
        .pluck('Items')
        .flatten()
        .value();

    items.forEach(function(item) {
        if (itemsToFind.indexOf(item.Id) > -1) toReturn.push(item);
    })
    
    return toReturn;
}

function formatDiscounts(rawDiscounts) {
    var toReturn = [];
    rawDiscounts.forEach(function(elem) {
        var obj = {
            "name": elem.Name,
            "id": elem.Id,
            "picture": _.where(elem.Pictures, {"ContentType": "COT_PIC_ACTION_LIST"})[0].Path || ""
        };
        toReturn.push(obj);
    })
    return toReturn;
}

function formatCategories(rawCatalog) {
    var toReturn = [];
    rawCatalog.forEach(function(elem) {
        var obj = {
            "id": elem.Id,
            "name": elem.Name,
            "Sections": elem.Sections
        };
        toReturn.push(obj);
    })
    return toReturn;
}

function formatSections(rawSections) {
    var toReturn = [];
    rawSections.forEach(function(elem) {
        var obj = {
            "id": elem.Id,
            "name": elem.Name,
            "Items": elem.Items
        };
        toReturn.push(obj);
    })
    return toReturn;
}

function formatItems(rawItems) {
    var toReturn = [];
    rawItems.forEach(function(elem, index) {
        var obj = {
            "id": elem.Id,
            "nameToDisplay": elem.Name,
            "desc": elem.Desc,
            "unitCount": elem.UnitCount,
            "unitName": elem.UnitName,
            "price": elem.Price,
            "picture": elem.Picture[0] || ""
        };
        obj['name'] = getChannelType("whatsapp") ? "Выбрать товар" : "Товар " + (index + 1);
        toReturn.push(obj);
    })
    return toReturn;
}

// Функция поиска элемента в массиве по полю name
function findItem(array, name) {
    return _.chain(array)
        .flatten()
        .find(function(elem) { return clearSymbols(elem.name) === clearSymbols(name); })
        .value();
}

// Функция отправки изображения с текстом в чат
function sendImage(url, text) {
    text = text || "";
    $.response.replies = $.response.replies || [];
    $.response.replies.push( {
        "type": "image",
        "imageUrl": url,
        "text": text
    });
}

// Функция для создания и показа кнопок с возможностью пагинации
function paginateButtons(catalog, settings) {
    var buttons = [];
    
    if (settings.page > 0) {
        if (getChannelType() === "whatsapp" && settings.page < catalog.length) {
            buttons.push({"text": "Назад", "transition": settings.onPreviousButtonTransition});
        }
        
        if (getChannelType() !== "whatsapp" && settings.page < catalog.length - 1) {
            buttons.push({"text": "Назад", "transition": settings.onPreviousButtonTransition});
        }
    }
    
    var elementsOnCurrentPage = catalog[settings.page];
    elementsOnCurrentPage.forEach(function(elem) {
        buttons.push({"text": elem.name, "transition": settings.onButtonTransition});
        if (settings.onButtonAction) settings.onButtonAction(elem);
    });
    
    if (!catalog[settings.page + 1]) {
        if (catalog.length > 1) buttons.push({"text": "В начало", "transition": settings.onRefreshButtonTransition});
    } else {
        buttons.push({"text": "Вперёд", "transition": settings.onNextButtonTransition});
    }
    
    $reactions.buttons(buttons);
}

// Функция формирования ответа из пришедшего от апи последнего заказа
function lastOrderAnswer(lastOrder) {
    var message = 'Заказ №' + lastOrder.id + ' имеет статус "' + lastOrder.statusname + '"\n'
        + 'Состоит из:';
        
    lastOrder.items.forEach(function(item) {
        message += '\n - ' + item.name + ' в количестве ' + item.unitcount + item.unitname + ' стоимостью ' + item.unitprice;
    });

    message += '\nАдрес доставки: ' + lastOrder.deliveryaddress +
        '\nДата и время: ' + lastOrder.deliverydate +
        '\nБутылей к возврату: ' + lastOrder.debt +
        '\nОплата: ' + lastOrder.paymenttypes[0].payname +
        '\n\nСумма к оплате: ' + lastOrder.cost;
        
    return message;
}

// Функция форматирования и формирования ответа их пришедшей от апи отдельной акции. Также здесь отправляем картинку этой акции
function discountAnswer(rawDiscount) {
    var picture = rawDiscount.Picture;
    var text = removeTags(rawDiscount.Text);
    text = rawDiscount.Name + "\n" + text;
    sendImage(picture, text);
}

function removeTags(text) {
    var regExps = {
        '<.*title.*>': '', // убрать title
        '(<([^>]+)>)': '', // убрать все html тэги
        '  ': '',
        '\r': '\n',
        '\n \n': '\n\n',
        '\n\n+': '\n\n'
    };
    
    for (var re in regExps) {
        text = text.replace(new RegExp(re, 'g'), regExps[re]);
    }
    
    return text;
}

function answerCategories(categories) {
    var text = "В нашем каталоге представлены следующие категории:";
    categories.forEach(function(category) {
        text += '\n' + " - " + category.Name;
    })
    text += '\n' + "Что именно вас интересует?";
    
    $reactions.answer(text);
}

function onPaginationItemAction(item) {
    var text = item.name + 
        '\n' + item.nameToDisplay + 
        '\n' + item.unitCount + " " + item.unitName +
        '\n' + item.desc +
        '\n\n' + item.price;
    sendImage(item.picture);
    $reactions.answer(text);
}

var pagination = new function() {
    this.getPage = function(key) {
        $.session.lastPaginationKey = key;
        return  $.session.paginationPage[key] || 0;
    }
    
    this.setPage = function(key, value) {
        $.session.paginationPage[key] = value;
    }
    
    this.increasePage = function(key) {
        var page = this.getPage(key);
        this.setPage(key, page + 1);
    }
    
    this.decreasePage = function(key) {
        var page = this.getPage(key);
        this.setPage(key, page - 1);
    }
    
    this.resetPage = function(key) {
        this.setPage(key, 0);
    }
}

function clickableLink(text, url) {
    return '<a href=\"' + url +'" target=\"_blank\">' + text + '</a>';
}

var transferData = new function() {
    this.formatClient = function() {
        return {
            "ID клиента": $.client.id || null,
            "Имя": $.client.name || null,
            "Адрес": $.client.address || null,
            "utmId": $.client.utmId || null
        };
    }
    
    this.formatItem = function(item) {
        var formatted =  {
            "ID товара": item.id,
            "Название товара": item.nameToDisplay,
            "Описание товара": item.desc,
            "Количество товара": item.unitCount,
            "Единица измерения товара": item.unitName,
            "Стоимость товара": item.price,
            "Изображение товара": item.picture
        }
        return _.extend(this.formatClient(), formatted);
    }
    
    this.formatDiscount = function(discount) {
        var formatted =  {
            "ID акции": discount.Id,
            "Название акции": discount.Name,
            "Описание акции": removeTags(discount.Text),
            "Условия акции": removeTags(discount.TextDetails),
            "Изображение акции": discount.Picture
        }
        return _.extend(this.formatClient(), formatted);
    }
}