package io.choerodon.manager.app.service.impl


import com.fasterxml.jackson.databind.ObjectMapper
import io.choerodon.core.exception.CommonException
import io.choerodon.eureka.event.EurekaEventPayload
import io.choerodon.manager.IntegrationTestConfiguration
import io.choerodon.manager.MockBeanTestConfiguration
import io.choerodon.manager.api.eventhandler.EurekaEventObserver
import io.choerodon.manager.app.service.ActuatorRefreshService
import io.choerodon.manager.app.service.DocumentService
import io.choerodon.manager.app.service.RouteService
import io.choerodon.manager.app.service.SwaggerRefreshService
import io.choerodon.manager.domain.manager.entity.RouteE
import io.choerodon.manager.domain.repository.RouteRepository
import io.choerodon.manager.domain.service.IRouteService
import io.choerodon.manager.infra.dto.RouteDTO
import org.springframework.beans.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import spock.lang.Shared
import spock.lang.Specification

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

/**
 * @author dengyouquan
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import([IntegrationTestConfiguration, MockBeanTestConfiguration])
class RouteServiceImplSpec extends Specification {

    @Shared
    private ObjectMapper objectMapper

    @Autowired
    private RouteRepository mockRouteRepository

    private IRouteService mockIrouteService = Mock(IRouteService)

    private RouteService routeService

    @Shared
    RouteDTO createdRouteDTO
    @Shared
    RouteDTO nameDuplicateRouteDTO
    @Shared
    RouteDTO pathDuplicateRouteDTO

    def setup() {
        routeService = new RouteServiceImpl(mockIrouteService, mockRouteRepository)
    }

    def setupSpec() {
        objectMapper = new ObjectMapper()
        String routeDTOJson = '{"id":10,"name":"test","path":"/test/**","serviceId":"test-service"}'
        createdRouteDTO = objectMapper.readValue(routeDTOJson, RouteDTO)
        nameDuplicateRouteDTO = new RouteDTO()
        BeanUtils.copyProperties(createdRouteDTO, nameDuplicateRouteDTO)
        nameDuplicateRouteDTO.setName("iam")
        pathDuplicateRouteDTO = new RouteDTO()
        BeanUtils.copyProperties(createdRouteDTO, pathDuplicateRouteDTO)
        pathDuplicateRouteDTO.setPath("/iam/**")
    }

//    def "List"() {
//        given: "构建RouteDO和PageRequest"
//        def routeDO = new RouteDTO()
//        def params = ""
//
//        when: "调用list方法"
//        routeService.list(0,10, routeDO, params)
//
//        then: "校验状态码和调用次数"
//        1 * mockIrouteService.pageAll(0,10, routeDO, params) >> { Mock(PageInfo) }
//    }

    def "Create"() {
        given: "创建正确的RouteDTO对象"
        def routeDTO = createdRouteDTO

        when: "调用创建routeDTO方法"
        routeService.create(routeDTO)

        then: "校验调用次数"
        1 * mockRouteRepository.addRoute(_ as RouteE)
        0 * _
    }

    def "Update"() {
        given: "构造RouteDTO"
        def updateRouteDTO = createdRouteDTO

        when: "调用更新routeDTO方法"
        routeService.update(updateRouteDTO.getId(), updateRouteDTO)

        then: "校验调用次数"
        1 * mockRouteRepository.updateRoute(_ as RouteE)
        0 * _
    }

    def "Delete"() {
        given: "构造routeDTO"
        def deletedRouteDTO = createdRouteDTO

        when: "调用删除routeDTO方法"
        routeService.delete(deletedRouteDTO.getId())

        then: "校验调用次数"
        noExceptionThrown()
    }

//    def "AddRoutesBatch"() {
//        given: "创建正确的List<RouteDTO>对象"
//        String routeDTOListJson = '[{"id":10,"name":"testdyq","path":"/testdyq/**","serviceId":"testdyq-service"}' +
//                ',{"id":11,"name":"test1","path":"/testdyq1/**","serviceId":"testdyq1-service"}]'
//        List<RouteDTO> routeDTOList = objectMapper.readValue(routeDTOListJson, new TypeReference<List<RouteDTO>>() {})
//
//        when: "调用创建routeDTO方法"
//        routeService.addRoutesBatch(routeDTOList)
//
//        then: "校验调用次数"
//        1 * mockIrouteService.addRoutes(_)
//        0 * _
//    }

//    def "GetAllRoute"() {
//        when: "调用获取所有routeDTO方法"
//        routeService.getAllRoute()
//
//        then: "校验调用次数"
//        1 * mockIrouteService.getAll()
//        0 * _
//    }

//    def "QueryByName"() {
//        when: "调用通过名字查找routeDTO方法"
//        routeService.queryByName(createdRouteDTO.getName())
//
//        then: "校验调用次数"
//        1 * mockRouteRepository.queryRoute(_)
//        0 * _
//    }

    def "CheckRoute"() {
        given: "构造Route"
        def routeDTO = createdRouteDTO

        when: "调用正常checkRoute方法"
        routeService.checkRoute(routeDTO)

        then: "校验"
        noExceptionThrown()

        when: "调用抛出异常的checkRoute方法"
        routeService.checkRoute(duplicateRouteDTO)

        then: "校验异常信息和调用次数"
        (1..2) * mockRouteRepository.countRoute(_) >> {
            RouteDTO r -> (r.getName().equals(nameDuplicateRouteDTO.getName()) || r.getPath().equals(pathDuplicateRouteDTO.getPath())) ? 1 : 0
        }
        def error = thrown(expectedException)
        error.message == expectedMessage

        where: "异常对比"
        duplicateRouteDTO     || expectedException | expectedMessage
        nameDuplicateRouteDTO || CommonException   | "error.route.insert.nameDuplicate"
        pathDuplicateRouteDTO || CommonException   | "error.route.insert.pathDuplicate"
    }
}
