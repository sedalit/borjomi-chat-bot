theme: /

    state: Products
        if: getChannelType("chatwidget")
            a: Ознакомиться с товарами вы можете на соответствующей странице {{clickableLink("сайта", "https://mywatershop.ru")}}
        else:
            if: !$client.id
                script: $session.cameFrom = "Items";
                go!: /Identification
            else:
                script: $session.catalog = integration.getCatalog();
                if: !$session.catalog
                    if: $temp.apiStatus === 401
                        script: $session.cameFrom = "Items";
                        go!: /Identification
                    else:
                        script: $temp.counter = counter("products")
                        if: $temp.counter < 3
                            go!: /Products
                        else:
                            go!: /IntegrationFailed
                else:
                    go!: /LastOrderCatalog
                    
    state: LastOrderCatalog
        if: !integration.getLastOrder();
            go!: /Catalog
        else:
            script: $temp.orders = integration.getOrders(2);
            if: !$temp.orders
                if: counter("lastOrderCatalog") < 3
                    go!: /LastOrderCatalog
                else:
                    go!: /Catalog
            else:
                script: $temp.orders = findItemsInCatalog($session.catalog, $temp.orders);
                a: Вижу, что вы уже делали заказ. Можем повторить или перейти в общий каталог, чтобы посмотреть что-нибудь новое.
                script: answerLastOrderCatalog($temp.orders)
                    
        state: Fallback
            event: noMatch
            script: $temp.counter = counter("lastOrderCatalogFallback");
            if: $temp.counter === 1
                a: Извините, не совсем понял. Выберите, пожалуйста, интересующий товар или перейдите в каталог
                go!: ..
            elseif: $temp.counter === 2
                a: К сожалению, не смог понять, что вы имеете в виду. Выберите интересующий для заказа товар или перейдите в каталог
                go!: ..
            else:
                a: К сожалению, я не могу помочь вам с этим
                go!: /TransferToOperator
            
    state: Catalog
        script:
            answerCategories($session.catalog);
            $session.categories = formatCategories($session.catalog);
            $temp.paginatableCategories = catalog.create($session.categories);
            paginateButtons($temp.paginatableCategories, {
                "page": pagination.getPage("catalog"),
                "onButtonTransition": "/Catalog/Fallback",
                "onNextButtonTransition": "/Pagination/OnNextButton",
                "onPreviousButtonTransition": "/Pagination/OnPreviousButton",
                "onRefreshButtonTransition": "/Pagination/OnRefreshButton"
            });
        
        state: Fallback
            event: noMatch
            if: findItem($session.categories, $request.query)
                script: $session.category = findItem($session.categories, $request.query);
                go!: /Sections
            else:
                script: $temp.counter = counter("catalog");
                if: $temp.counter === 1
                    a: Извините, не совсем понял. Выберите, пожалуйста, интересующий товар или перейдите в каталог
                    go!: ..
                elseif: $temp.counter === 2
                    a: К сожалению, не смог понять, что вы имеете в виду. Выберите интересующий для заказа товар или перейдите в каталог
                    go!: ..
                else:
                    a: К сожалению, я не могу помочь вам с этим
                    go!: /TransferToOperator
            
    state: Sections
        if: $session.category['Sections']
            script: 
                $session.sections = formatSections($session.category['Sections']);
                $temp.paginatableSections = catalog.create($session.sections);
            a: В выбранной категории есть несколько разделов. Пожалуйста, выберите подходящий:
            script: 
                paginateButtons($temp.paginatableSections, {
                    "page": pagination.getPage("sections"),
                    "onButtonTransition": "/Sections/Fallback",
                    "onNextButtonTransition": "/Pagination/OnNextButton",
                    "onPreviousButtonTransition": "/Pagination/OnPreviousButton",
                    "onRefreshButtonTransition": "/Pagination/OnRefreshButton"
                });
        else:
            go!: /Items
        
        state: Fallback
            event: noMatch
            if: findItem($session.sections, $request.query)
                script: $session.section = findItem($session.sections, $request.query);
                go!: /Items
            else:
                script: $temp.counter = counter("sections")
                if: $temp.counter === 1
                    a: Извините, не совсем понял. Выберите, пожалуйста, раздел с интересующими вас товарами?
                    go!: ..
                elseif: $temp.counter === 2
                    a: К сожалению, не смог понять, что вы имеете в виду. Выберите раздел, товары из которого хотите посмотреть?
                    go!: ..
                else:
                    a: К сожалению, я не могу помочь вам с этим
                    go!: /TransferToOperator
                
    state: Items
        if: $session.section['Items'] || $session.category['Items']
            script: 
                var currentItems = $session.section['Items'] || $session.category['Items'];
                $session.items = formatItems(currentItems);
            if: getChannelType("whatsapp")
                script: $session.paginatableItems = catalog.oneItemPerPage($session.items);
            else:
                script: $session.paginatableItems = catalog.create($session.items);
            a: В выбранном разделе есть несколько товаров. Что вас интересует? Вы можете просмотреть каталог используя кнопки-стрелки. Выбор товара приведет к переводу на оператора для оформления заказа
            script:
                paginateButtons($session.paginatableItems, {
                    "page": pagination.getPage("items"),
                    "onButtonTransition": "/SpecificItem",
                    "onButtonAction": function(elem){onPaginationItemAction(elem)},
                    "onNextButtonTransition": "/Pagination/OnNextButton",
                    "onPreviousButtonTransition": "/Pagination/OnPreviousButton",
                    "onRefreshButtonTransition": "/Pagination/OnRefreshButton"
                });
        else:
            a: К сожалению, в этом разделе товары отсутствуют. Посмотрите, пожалуйста, другие товары
            go!: /Catalog
        
        state: Fallback
            event: noMatch
            script: $temp.counter = counter("items");
            if: $temp.counter === 1
                a: Извините, не совсем понял. Выберите, пожалуйста, товар, который хотели бы заказать?
                go!: ..
            elseif: $temp.counter === 2
                a: К сожалению, не смог понять, что вы имеете в виду. Выберите товар для заказа
                go!: ..
            else:
                a: К сожалению, я не могу помочь вам с этим
                go!: /TransferToOperator
        
    state: SpecificItem
        script: 
            $temp.item = _.find($session.paginatableItems[$session.paginationPage.items], function(item){ return item.name === $request.query });
            $session.transferData = transferData.formatItem($temp.item);
        a: Передаю данные о заказе оператору. Подождите пару минут, ваш запрос обрабатывается
        go!: /Switch