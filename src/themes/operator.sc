theme: /

    state: TransferToOperator
        q: ($yes|$agree) || fromState = "/GetNumber/Error"
        q!: * (оператор*/человек*/живого) *
        q!: * {(не (помог*/ответ*/тот ответ*)/~предложение/человек*) * (телефон*/адрес*/мыло/почта/мэйл/email/электрон* ~адрес/контактн* ~лицо/связаться/*звонить)}*
        q!: * {(соедин* [меня]|позови*|связать*|свяжи* [меня]|дозвонить*|позвонить|звонить|поговори*|переключ*|[мне] нужен|чат|[номер] для связи|связь*|телефон*|есть) * [с|со|до|на] * ($serviceHelperHuman|~другой)} *
        q!: * не [с] (робот*|бот*|~автомат) *
        q!: * {(живог*|~живой) * (позов*|$need)} *
        q!: * (техподдержк*|тех* поддержк*) *
        random:
            a: Связываю с менеджером, ожидайте.
            a: Подождите, пожалуйста, менеджер сейчас подключится.
        script: if (!$session.transferData) $session.transferData = transferData.formatClient();
        go!: /Switch
     
    state: Switch
        script: transferToOperator($response);
    
        state: DialogEnd
            event: livechatFinished
            q: $regexp_i</close>
            script: $jsapi.stopSession();
                
    state: TransferError
        event!: noLivechatOperatorsOnline
        if: counter("transferError") < 2
            go!: /Switch
        else:
            random:
                a: К сожалению, в данный момент не могу соединить вас с оператором. Вы можете самостоятельно связаться с ним по номеру 8 (800) 551 67 21
                a: Простите, не смог соединить вас с оператором. Вы можете связаться с ним самостоятельно по номеру 8 (800) 551 67 21
            go!: /SomethingElse