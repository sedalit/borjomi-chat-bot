$global.$ = {
    __noSuchProperty__: function(property) {
        return $jsapi.context()[property];
    }
};

bind("postProcess", function() {
    if (!$.currentState.startsWith("/Pagination")) $.session.lastState = $.currentState;
});

bind("onAnyError", function() {
    $reactions.answer("Кажется, произошла ошибка. Подождите, перевожу вас на менеджера");
    $.session.transferData = $.session.transferData || transferData.formatClient();
    transferToOperator($.response);
});