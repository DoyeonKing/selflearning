/**
 * 应用配置文件
 * 
 * ⚠️ 真机调试必读：
 * 1. 手机无法访问 localhost，必须改为电脑的局域网IP
 * 2. 获取IP方法：
 *    - Windows: 运行 ipconfig，查看"IPv4 地址"
 *    - Mac/Linux: 运行 ifconfig，查看 inet 地址
 * 3. 确保手机和电脑连接同一个WiFi
 * 4. 确保后端服务正在运行（springboot在8080端口）
 * 
 * 常见IP格式：192.168.x.x 或 10.x.x.x
 */

// ⚠️ 真机调试：请修改为你的电脑局域网IP地址
// 检测到的IP地址：
//   - 192.168.137.1 (可能是手机热点)
//   - 172.20.10.3 (可能是WiFi连接) ← 试试这个！
//   - 26.206.1.21 (可能是虚拟网卡)

// ⚠️ 重要：根据你的网络环境选择正确的IP
// 
// 检测到的IP地址：
//   1. 192.168.137.1 - 手机热点（如果电脑连接手机热点用这个）
//   2. 172.20.10.3 - WiFi连接（如果手机和电脑在同一WiFi用这个）
//   3. 26.206.1.21 - 虚拟网卡（通常不用）

// 当前使用：iPhone热点IP
const LOCAL_IP = '172.20.10.3' // 👈 电脑连接iPhone热点后的IP地址

// 方案3：如果都不行，手动查找：
// 1. 打开 cmd，运行 ipconfig
// 2. 找到连接WiFi的那个网卡的IPv4地址
// 3. 通常是 192.168.1.x 或 192.168.0.x

// 开发环境配置
const development = {
	// 真机调试使用局域网IP
	baseURL: `http://${LOCAL_IP}:8080`
	
	// 如果要在浏览器测试，可以临时改为：
	// baseURL: 'http://localhost:8080'
}

// 生产环境配置
const production = {
	baseURL: 'https://your-production-api.com'
}

// 当前使用的配置
const config = development

// 同时支持 default 和 named export
export default config
export { config }

