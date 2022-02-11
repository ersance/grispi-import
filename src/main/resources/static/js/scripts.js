/*!
* Start Bootstrap - Simple Sidebar v6.0.3 (https://startbootstrap.com/template/simple-sidebar)
* Copyright 2013-2021 Start Bootstrap
* Licensed under MIT (https://github.com/StartBootstrap/startbootstrap-simple-sidebar/blob/master/LICENSE)
*/
// 
// Scripts
// 

window.addEventListener('DOMContentLoaded', event => {

    // Toggle the side navigation
    const sidebarToggle = document.body.querySelector('#sidebarToggle');
    if (sidebarToggle) {
        // Uncomment Below to persist sidebar toggle between refreshes
        // if (localStorage.getItem('sb|sidebar-toggle') === 'true') {
        //     document.body.classList.toggle('sb-sidenav-toggled');
        // }
        sidebarToggle.addEventListener('click', event => {
            event.preventDefault();
            document.body.classList.toggle('sb-sidenav-toggled');
            localStorage.setItem('sb|sidebar-toggle', document.body.classList.contains('sb-sidenav-toggled'));
        });
    }

    const zendeskConfigForm = document.body.querySelector('#zendesk-config');
    const zendeskImportStatus = document.body.querySelector('#zendeskImportStatus');
    zendeskConfigForm.addEventListener("submit", event => {
        event.preventDefault();

        fetch("/zendesk-import", {
            method: "POST",
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                "zendeskApiCredentials": {
                    "subdomain": event.target.elements['zendeskApiCredentials.subdomain'].value,
                    "email": event.target.elements['zendeskApiCredentials.email'].value,
                    "token": event.target.elements['zendeskApiCredentials.token'].value,
                },
                "grispiApiCredentials": {
                    "tenantId": event.target.elements['grispiApiCredentials.tenantId'].value,
                    "token": event.target.elements['grispiApiCredentials.token'].value,
                }
            })
        })
            .then(response => response.json())
            .then(data => {
                console.log("Request complete! response:", data);

                document.body.querySelector("#operation-id").innerHTML = data['id']

                for (const [key, value] of Object.entries(data['resources'])) {
                    console.log(`${key}: ${value}`);
                    document.body.querySelector("#zendesk-resources").innerHTML +=
                        '<li class="list-group-item d-flex justify-content-between align-items-center">' +
                        key + '<span class="badge bg-primary rounded-pill\">'+ value +'</span></li>';
                }

                zendeskImportStatus.className = 'collapse show'

                zendeskConfigForm.className = 'collapse'
        });
    })

});
