theme: /Pagination

    state: OnNextButton
        script: pagination.increasePage($session.lastPaginationKey);
        go!: {{$session.lastState}}
                
    state: OnPreviousButton
        script: pagination.decreasePage($session.lastPaginationKey);
        go!: {{$session.lastState}}
            
    state: OnRefreshButton
        script: pagination.resetPage($session.lastPaginationKey);
        go!: {{$session.lastState}}