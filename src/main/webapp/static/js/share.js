/*
 * 글 상세의 공유 버튼.
 *
 * 페이스북과 쓰레드는 링크(intent URL)만으로 공유되므로 JS 없이도 동작한다.
 * 이 파일이 맡는 것은 링크로 처리할 수 없는 세 가지다.
 *
 *   1) 네이티브 공유 - navigator.share. 모바일에서 OS 공유 시트를 띄운다.
 *      인스타그램과 카카오톡은 웹 공유 URL 이 없어 이 경로로만 보낼 수 있다.
 *   2) 링크 복사    - 어디서나 통하는 최종 수단.
 *   3) 카카오톡     - Kakao SDK 가 로드된 경우에만(= BLOG_KAKAO_JS_KEY 설정 시).
 */
(function () {
  'use strict';

  var box = document.querySelector('[data-share]');
  if (!box) return;

  var url = box.getAttribute('data-url') || location.href;
  var title = box.getAttribute('data-title') || document.title;

  // 1) 네이티브 공유. 지원하는 브라우저에서만 버튼을 드러낸다.
  var nativeBtn = box.querySelector('.share-native');
  if (nativeBtn && navigator.share) {
    nativeBtn.hidden = false;
    nativeBtn.addEventListener('click', function () {
      // 사용자가 공유 시트를 닫으면 reject 된다. 오류가 아니므로 무시한다.
      navigator.share({ title: title, url: url }).catch(function () {});
    });
  }

  // 2) 링크 복사
  var copyBtn = box.querySelector('.share-copy');
  var toast = box.querySelector('.share-toast');
  if (copyBtn) {
    copyBtn.addEventListener('click', function () {
      copyText(url).then(function (ok) {
        flash(ok ? '링크를 복사했습니다' : '복사하지 못했습니다');
      });
    });
  }

  function copyText(text) {
    if (navigator.clipboard && window.isSecureContext) {
      return navigator.clipboard.writeText(text).then(
        function () { return true; },
        function () { return false; }
      );
    }
    // clipboard API 를 못 쓰는 환경(구형 브라우저, http) 폴백
    var ta = document.createElement('textarea');
    ta.value = text;
    ta.setAttribute('readonly', '');
    ta.style.position = 'fixed';
    ta.style.top = '-1000px';
    document.body.appendChild(ta);
    ta.select();
    var ok = false;
    try { ok = document.execCommand('copy'); } catch (e) { ok = false; }
    document.body.removeChild(ta);
    return Promise.resolve(ok);
  }

  // 버튼이 아이콘 전용이라 글자를 바꿀 수 없다. 별도 알림 영역에 띄운다.
  // role="status" 라 스크린리더에도 읽힌다.
  var toastTimer = null;
  function flash(msg) {
    if (!toast) return;
    toast.textContent = msg;
    toast.classList.add('is-on');
    if (toastTimer) clearTimeout(toastTimer);
    toastTimer = setTimeout(function () {
      toast.classList.remove('is-on');
      toast.textContent = '';
    }, 2000);
  }

  // 3) 카카오톡. sendScrap 은 페이지의 og 태그를 그대로 긁어가므로
  //    공유 카드가 항상 본문 메타 정보와 일치한다.
  var kakaoBtn = box.querySelector('.share-kakao');
  if (!kakaoBtn) return;

  if (!window.Kakao) {          // SDK 가 안 실려 있으면 버튼을 숨긴다
    kakaoBtn.hidden = true;
    return;
  }
  try {
    if (!window.Kakao.isInitialized()) {
      window.Kakao.init(kakaoBtn.getAttribute('data-key'));
    }
    kakaoBtn.addEventListener('click', function () {
      window.Kakao.Share.sendScrap({ requestUrl: url });
    });
  } catch (e) {
    kakaoBtn.hidden = true;   // 키가 잘못됐거나 도메인 미등록
  }
})();
