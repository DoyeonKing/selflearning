// 医生ID管理工具
export function setDoctorId(doctorId) {
  localStorage.setItem('doctorId', doctorId.toString());
}

export function getDoctorId() {
  const storedId = localStorage.getItem('doctorId');
  return storedId ? parseInt(storedId) : null;
}

export function clearDoctorId() {
  localStorage.removeItem('doctorId');
}

// 初始化时设置医生ID（临时方案）
export function initDoctorId() {
  if (!getDoctorId()) {
    // 如果没有存储的医生ID，设置为11（根据截图）
    setDoctorId(11);
  }
}
