<?xml version="1.0" encoding="UTF-8"?>
<html>
<head xmlns="http://www.w3.org/1999/xhtml" lang="en">
  <meta content="text/html; charset=utf-8" http-equiv="Content-Type"/>
  <meta content="telephone=no" name="format-detection"/>
  <meta content="width=device-width, initial-scale=1.0;" name="viewport"/>
  <style>
    body {
      font-family: Verdana;
      font-size: 12px;
    }
  </style>
</head>
<body>
<table marginheight="0" topmargin="0" marginwidth="0" width="100%" style="font-size: 14px;">
  <tr>
    <td align="center" width="1000%">
      <table width="100%" cellpadding="0" cellspacing="0">
        <tr>
          <th align="left">
            <h2 style="font-size: 18px; font-weight: normal; text-transform: capitalize;color: #898989;">
          ${challengeName}
            <span style="font-size: 25px; font-weight: normal; color:#1c7abc">$${totalPlaceReward?string["0.####"]}</span>
            </h2>
          <span style="font-size: 12px; font-weight: normal;color: #898989;">${challengeOverview}</span>
          </th>
        </tr>
        <tr>
          <td align="left">
            <br/>
            <table cellpadding="0" cellspacing="0" width="100%">
              <tr>
                <td height="1px" style="background-color: #337ab7;" width="34%"></td>
                <td height="1px" style="background-color: #8a2890" width="33%"></td>
                <td height="1px" style="background-color: #8d8d8d" width="33%"></td>
              </tr>
            </table>
          </td>
        </tr>
        <tr>
          <td align="left" style="height: 50px;"></td>
        </tr>
        <tr>
          <td align="center">
            <br/>
            <#if winnersInfo?size != 0>
              <span style="font-size: 12px; font-weight: normal"><strong style="font-size: 25px; color:#bf3a2f">${winnersInfo?size}</strong> Người chiến thắng</span>
              <#list winnersInfo as winnerInfo>
                <br/>
                  <span style="font-size: 14px; color: #bf3a2f; font-weight: normal">
                      <#if winnerInfo.registrantFirstName?has_content>${winnerInfo.registrantFirstName?capitalize}</#if>
                      <#if winnerInfo.registrantLastName?has_content>${winnerInfo.registrantLastName?capitalize}</#if>
                  </span>
              </#list>
            </#if>
          </td>
        </tr>
        <tr>
          <td>
            <br/>
        <#include "FINAL_CHALLENGE_REPORT_${phaseEntries?size}_PHASES_VI.ftl" encoding="UTF-8">
          </td>
        </tr>
        <tr>
          <td align="left" style="height: 250px;"></td>
        </tr>
        <tr>
          <td align="left" style="height: 1px; background-color: #8d8d8d;"></td>
        </tr>
        <tr>
          <td align="left" style="height: 5px;"></td>
        </tr>
        <tr>
          <td align="left" width="100%">
            <table width="100%" style="background-color:#fff">
              <tr>
                <td align="left" width="70%">
                  <h3 style="font-size: 14px; color: #898989; font-weight: 300; margin:0 0 5px 0">Thử Thách Trực Tuyến</h3>
                  <span style="font-size: 10px;  color: #898989;">Xây Dựng Đội Ngũ Phát Triển Sản Phẩm Của Chính Bạn Thông Qua Các Cuộc Thi Trực Tuyến.</span>
                </td>
                <td align="right" width="30%">
                  <img src="${baseUrl}images/logo.png" alt="" style="margin: auto; width: 150px"/>
                </td>
              </tr>
            </table>
          </td>
        </tr>
      </table>
    </td>
  </tr>
</table>
</body>
</html>