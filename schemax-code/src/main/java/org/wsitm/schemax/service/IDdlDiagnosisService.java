package org.wsitm.schemax.service;

import org.wsitm.schemax.entity.vo.DdlCheckRequestVO;
import org.wsitm.schemax.entity.vo.DdlCheckResultVO;

public interface IDdlDiagnosisService {
    DdlCheckResultVO precheck(DdlCheckRequestVO request);
}
