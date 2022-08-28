//
//  blob_converter.js
//  YSports
//
//  Copyright © 2022 YSports. All rights reserved.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//

function blobConvert(url, type, filename) {
    var xhr = new XMLHttpRequest();
    xhr.open('GET', url, true);
    xhr.setRequestHeader('Content-type', type);
    xhr.responseType = 'blob';
    xhr.onload = function (e) {
        if (this.status == 200) {
            var blob = this.response;
            var reader = new FileReader();
            reader.readAsDataURL(blob);
            reader.onloadend = function () {
                dataUrl = reader.result;
                Android.downloadBase64(dataUrl, filename);
            }
            reader.onerror = function () {
                Android.toast('Failed to load URL');
            }
        } else {
            Android.toast('Failed to load URL');
        }
    };
    xhr.send();
}
