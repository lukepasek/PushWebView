
function content_set(html)
{
    const elem = document.getElementById("content");
    elem.innerHTML = html;
}

function content_prepend(html)
{
    const elem = document.getElementById("content");
    elem.insertAdjacentHTML('afterbegin', html);
}

function content_append(html)
{
    const elem = document.getElementById("content");
    elem.insertAdjacentHTML('beforeend', html);
}

function update_clock(time) {
    const elem = document.getElementById("clock");
    elem.innerHTML = time;
}

function set_wifi_info(html)
{
    const elem = document.getElementById("wifi-info");
    elem.innerHTML = html;
}

function update_values(data) {
    if (data) {
        console.log(data);
        var i = 0;
        const elements = data.split(/ +/);
        while (i < elements.length) {
            const parts = elements[i].split(':');
            const id = 'value-'+parts[0];
            const de = document.getElementById(id);
            if (de) {
                de.innerHTML = parts[1];
            } else {
//                id = 'data-cell-'+i;
//                de = document.getElementById(id);
//                if (de) {
//                    de.innerHTML = parts[1];
//                }
//                i += 1;
            }
        }
    }
}

var selected = null

function clickAt(x, y, action) {
    var element = document.elementFromPoint(x, y);
    console.log("click "+action+" at: " +x+"x"+y+", element: "+element);
    click(element, action);
}

function click(e, action) {
    if (action==1 && selected) {
        selected.style.backgroundColor = "initial";
        var p = selected;
        while (p) {
            if (p.classList.contains('icon-bg')) {
//                p.style.borderStyle = "solid";
//                p.style.borderColor = "initial";
                p.style.backgroundColor = "#04631e";


//                  p.style.opacity = "1";
                break;
            }
            p = p.parentElement;
        }
        selected = null;
    }
    if (e && e.onclick && action==0) {
        var p = e;
        while (p) {
            if (p.classList.contains('icon-bg')) {
//                p.style.borderStyle = "none";
                p.style.backgroundColor = "#02330f";
//                p.style.opacity = "0.3";
                break;
            }
            p = p.parentElement;
        }
        e.onclick();
//        e.style.backgroundColor = "rgba(0,0,0,0.5)";
        selected = e;
    }
}

function on_load() {
    /* android.toast("Document load"); */
    console.log("Document load");
    w = window.innerHeight;
    h = window.innerWidth;
    console.log("Window size: "+w+"x"+h);
}
