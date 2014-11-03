function isEmpty(inp) {
    var contact = new RegExp("^[0-9]{8}$");
    var id = inp.attr('id');
    var val = inp.val();
    if ((id === "inputContact" || id === "inputPhone")
        && !contact.test(val)) {
        document.getElementById(id).parentElement.parentElement.className = "form-group has-error";
    } else {
        if (val === "") {
            document.getElementById(id).parentElement.parentElement.className = "form-group has-error";
        } else if (val !== "") {
            document.getElementById(id).parentElement.parentElement.className = "form-group has-success";
        }
    }
}

function checkOverlay() {
    if (document.getElementById('landmark').checked) {
        document.getElementById('phOverlay_landmark').style.display = "block";
        document.getElementById('phOverlay_mrtstn').style.display = "none";
        document.getElementById('phOverlay_hdbtown').style.display = "none";
    } else if (document.getElementById('mrtstn').checked) {
        document.getElementById('phOverlay_landmark').style.display = "none";
        document.getElementById('phOverlay_mrtstn').style.display = "block";
        document.getElementById('phOverlay_hdbtown').style.display = "none";
    } else if (document.getElementById('hdbtown').checked) {
        document.getElementById('phOverlay_landmark').style.display = "none";
        document.getElementById('phOverlay_mrtstn').style.display = "none";
        document.getElementById('phOverlay_hdbtown').style.display = "block";
    }
}

function selectLostType(inp) {
    //window.alert(inp.id);
    document.getElementById("typeSelect").innerHTML = inp.html() + "<span class='caret'></span>";
    document.getElementById("typeSelectHidden").value = inp.html().trim();
    document.forms['lost-type-form'].submit();
}


// onmousemove="overlayImg(this,event)"
//function overlayImg(inp, event) {
//    var clientRect = inp.getBoundingClientRect();
//    var left = clientRect.left;
//    var top = clientRect.top;
//    var right = clientRect.right;
//    var bottom = clientRect.bottom;
//    
//    //window.alert(event.clientX + "," + event.clientY);
//    //LimChuKang = (left+84, top+77, right-475, bottom-186)
//    if (event.clientX > left + 84 &&
//        event.clientY > top + 77 &&
//        event.clientX < right - 475 &&
//        event.clientY < bottom - 186) {
//        var div = document.createElement('div');
//        div.style.left = '110px';
//        div.style.top =  '105px';
//        div.style.position = 'absolute';
//        div.style.display = 'block';
//        
//        var img = document.createElement('img');
//        img.src = "favicon.ico";
//        img.className = "img-responsive";
//        div.appendChild(img);
//        
//        inp.parentNode.appendChild(div);
//    }
//    //window.alert(inp.getBoundingClientRect().left);
//    //window.alert(event.clientX);
//}